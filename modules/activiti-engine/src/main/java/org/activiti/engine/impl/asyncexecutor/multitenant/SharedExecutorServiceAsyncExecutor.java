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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.activiti.engine.impl.asyncexecutor.AsyncExecutor;
import org.activiti.engine.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.activiti.engine.impl.asyncexecutor.ExecuteAsyncRunnableFactory;
import org.activiti.engine.impl.cfg.multitenant.TenantInfoHolder;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.LockedJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  public void addTenantAsyncExecutor(String tenantId, boolean startExecutor) {

    TenantAwareAcquireJobsDueRunnable asyncJobsRunnable = new TenantAwareAcquireJobsDueRunnable(this, tenantInfoHolder, tenantId);
    asyncJobAcquisitionRunnables.put(tenantId, asyncJobsRunnable);
    asyncJobAcquisitionThreads.put(tenantId, new Thread(asyncJobsRunnable));
    
    if (startExecutor) {
      startAsyncJobAcquisitionForTenant(tenantId);
    }
  }
  
  @Override
  protected void startJobAcquisitionThread() {
    
    for (String tenantId : asyncJobAcquisitionThreads.keySet()) {
      asyncJobAcquisitionThreads.get(tenantId).start();
    }
  }


  protected  void startAsyncJobAcquisitionForTenant(String tenantId) {
    asyncJobAcquisitionThreads.get(tenantId).start();
  }
  
  @Override
  protected void stopJobAcquisitionThread() {
    
    // Runnables
    
    for (String tenantId : asyncJobAcquisitionRunnables.keySet()) {
      asyncJobAcquisitionRunnables.get(tenantId).stop();
    }
    
    // Threads
    for (String tenantId : asyncJobAcquisitionThreads.keySet()) {
      try {
        asyncJobAcquisitionThreads.get(tenantId).join();
      } catch (InterruptedException e) {
        logger.warn("Interrupted while waiting for the timer job acquisition thread to terminate", e);
      }

    }
  }

}
