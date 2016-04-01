/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.engine.impl.asyncexecutor.multitenant;

import org.activiti.engine.impl.asyncexecutor.AsyncExecutor;
import org.activiti.engine.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.activiti.engine.impl.asyncexecutor.ExecuteAsyncRunnableFactory;
import org.activiti.engine.impl.cfg.multitenant.TenantInfoHolder;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.persistence.entity.LockedJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Multi tenant {@link AsyncExecutor}.
 * 
 * For each tenant, there will be acquire threads, but only one {@link ExecutorService} will be used
 * once the jobs are acquired.
 * 
 * @author Joram Barrez
 */
public class SharedExecutorServiceAsyncExecutor extends DefaultAsyncJobExecutor implements TenantAwareAsyncExecutor {
  
  private static final Logger logger = LoggerFactory.getLogger(SharedExecutorServiceAsyncExecutor.class);
  
  protected TenantInfoHolder tenantInfoHolder;

  protected Map<String, Thread> asyncJobAcquisitionThreads = new HashMap<String, Thread>();
  protected Map<String, TenantAwareAcquireJobsDueRunnable> asyncJobAcquisitionRunnables
    = new HashMap<String, TenantAwareAcquireJobsDueRunnable>();

  protected Map<String, Thread> cleanupJobsThreads = new HashMap<String, Thread>();
  protected Map<String, TenantAwareCleanupJobsRunnable> cleanupJobsRunables
    = new HashMap<String, TenantAwareCleanupJobsRunnable>();


  protected Map<String, Thread> timerJobMoveThreads = new HashMap<String, Thread>();
  protected Map<String, TenantAwareTimerJobsMoveRunnable> timerJobMoveRunables
          = new HashMap<String, TenantAwareTimerJobsMoveRunnable>();
  
  public SharedExecutorServiceAsyncExecutor(TenantInfoHolder tenantInfoHolder) {
    this.tenantInfoHolder = tenantInfoHolder;
    
    setExecuteAsyncRunnableFactory(new ExecuteAsyncRunnableFactory() {
      
      public Runnable createExecuteAsyncRunnable(LockedJobEntity jobEntity, CommandExecutor commandExecutor) {
        
        // Here, the runnable will be created by for example the acquire thread, which has already set the current id.
        // But it will be executed later on, by the executorService and thus we need to set it explicitely again then
        
        return new TenantAwareExecuteAsyncRunnable(jobEntity, commandExecutor, 
            SharedExecutorServiceAsyncExecutor.this.tenantInfoHolder, 
            SharedExecutorServiceAsyncExecutor.this.tenantInfoHolder.getCurrentTenantId());
      }
      
    });
  }

  @Override
  public Set<String> getTenantIds() {
    return asyncJobAcquisitionRunnables.keySet();
  }

  public void addTenantAsyncExecutor(String tenantId, boolean startExecutor) {

    TenantAwareAcquireJobsDueRunnable asyncJobsRunnable = new TenantAwareAcquireJobsDueRunnable(this, tenantInfoHolder, tenantId);
    asyncJobAcquisitionRunnables.put(tenantId, asyncJobsRunnable);
    asyncJobAcquisitionThreads.put(tenantId, new Thread(asyncJobsRunnable));

    TenantAwareCleanupJobsRunnable cleanupJobsRunnable = new TenantAwareCleanupJobsRunnable(this, tenantInfoHolder, tenantId);
    cleanupJobsRunables.put(tenantId, cleanupJobsRunnable);
    cleanupJobsThreads.put(tenantId, new Thread(asyncJobsRunnable));

    TenantAwareTimerJobsMoveRunnable timerJobsMoveRunnable = new TenantAwareTimerJobsMoveRunnable(this, tenantInfoHolder, tenantId);
    timerJobMoveRunables.put(tenantId, timerJobsMoveRunnable);
    timerJobMoveThreads.put(tenantId, new Thread(timerJobsMoveRunnable));

    if (startExecutor) {
      startAsyncJobAcquisitionForTenant(tenantId);
      startCleanupJobsForTenant(tenantId);
      startTimerJobMoveProcessorForTenant(tenantId);
    }
  }
  
  @Override
  public void removeTenantAsyncExecutor(String tenantId) {
    stopJobAcquisitionThreadsForTenant(tenantId);
  }

  @Override
  protected void startJobAcquisitionThread() {
    
    for (String tenantId : asyncJobAcquisitionThreads.keySet()) {
      asyncJobAcquisitionThreads.get(tenantId).start();
    }
  }


  @Override
  protected void startCleanupJobsThread() {

    for (String tenantId : cleanupJobsThreads.keySet()) {
      cleanupJobsThreads.get(tenantId).start();
    }
  }

  @Override
  protected void startTimerJobsMoveThread() {

    for (String tenantId : timerJobMoveThreads.keySet()) {
      timerJobMoveThreads.get(tenantId).start();
    }
  }

  protected  void startAsyncJobAcquisitionForTenant(String tenantId) {
    asyncJobAcquisitionThreads.get(tenantId).start();
  }

  protected  void startCleanupJobsForTenant(String tenantId) {
    cleanupJobsThreads.get(tenantId).start();
  }

  protected  void startTimerJobMoveProcessorForTenant(String tenantId) {
    timerJobMoveThreads.get(tenantId).start();
  }


  @Override
  protected void stopJobAcquisitionThread() {
    for (String tenantId : asyncJobAcquisitionRunnables.keySet()) {
      stopJobAcquisitionThreadsForTenant(tenantId);
    }
  }

  @Override
  protected void stopCleanupJobsThread() {
    for (String tenantId : cleanupJobsRunables.keySet()) {
      stopCleanupJobsThreadsForTenant(tenantId);
    }
  }

  @Override
  protected void stopTimerJobMoveThread() {
    for (String tenantId : timerJobMoveRunables.keySet()) {
      stopJobMoveThreadsForTenant(tenantId);
    }
  }

  protected void stopJobAcquisitionThreadsForTenant(String tenantId) {
    asyncJobAcquisitionRunnables.get(tenantId).stop();

    try {
      asyncJobAcquisitionThreads.get(tenantId).join();
    } catch (InterruptedException e) {
      logger.warn("Interrupted while waiting for the timer job acquisition thread to terminate", e);
    }
  }

  protected void stopJobMoveThreadsForTenant(String tenantId) {
    timerJobMoveRunables.get(tenantId).stop();

    try {
      timerJobMoveThreads.get(tenantId).join();
    } catch (InterruptedException e) {
      logger.warn("Interrupted while waiting for the timer job move thread to terminate", e);
    }
  }

  protected void stopCleanupJobsThreadsForTenant(String tenantId) {
    cleanupJobsRunables.get(tenantId).stop();

    try {
      cleanupJobsThreads.get(tenantId).join();
    } catch (InterruptedException e) {
      logger.warn("Interrupted while waiting for the cleanup jobs thread to terminate", e);
    }
  }

}
