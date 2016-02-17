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

package org.activiti.engine.impl.persistence.entity;

import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.impl.JobQueryImpl;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.cfg.TransactionListener;
import org.activiti.engine.impl.cfg.TransactionState;
import org.activiti.engine.impl.jobexecutor.AsyncJobAddedNotification;
import org.activiti.engine.impl.jobexecutor.JobAddedNotification;
import org.activiti.engine.impl.persistence.entity.data.DataManager;
import org.activiti.engine.impl.persistence.entity.data.JobDataManager;
import org.activiti.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Tom Baeyens
 * @author Daniel Meyer
 * @author Joram Barrez
 * @author Vasile Dirla
 */
public class GenericJobEntityManagerImpl extends AbstractEntityManager<JobEntity> implements GenericJobEntityManager {

  private static final Logger logger = LoggerFactory.getLogger(GenericJobEntityManagerImpl.class);

  protected JobDataManager jobDataManager;

  public GenericJobEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, JobDataManager jobDataManager) {
    super(processEngineConfiguration);
    this.jobDataManager = jobDataManager;
  }

  @Override
  protected DataManager<JobEntity> getDataManager() {
    return jobDataManager;
  }

  @Override
  public void insert(JobEntity jobEntity, boolean fireCreateEvent) {

    // add link to execution
    if (jobEntity.getExecutionId() != null) {
      ExecutionEntity execution = getExecutionEntityManager().findById(jobEntity.getExecutionId());
      execution.getJobs().add(jobEntity);

      // Inherit tenant if (if applicable)
      if (execution.getTenantId() != null) {
        jobEntity.setTenantId(execution.getTenantId());
      }
    }

    super.insert(jobEntity, fireCreateEvent);
  }

  @Override
  public void retryAsyncJob(JobEntity job) {
    try {

      // If a job has to be retried, we wait for a certain amount of time,
      // otherwise the job will be continuously be retried without delay (and thus seriously stressing the database).
      Thread.sleep(getAsyncExecutor().getRetryWaitTimeInMillis());

    } catch (InterruptedException e) {
    }
    getAsyncExecutor().executeAsyncJob(job);
  }

  protected void hintAsyncExecutor(JobEntity job) {

    // notify job executor:
    TransactionListener transactionListener = new AsyncJobAddedNotification(job, getAsyncExecutor());
    getCommandContext().getTransactionContext().addTransactionListener(TransactionState.COMMITTED, transactionListener);
  }

  protected void hintJobExecutor(JobEntity job) {

    // notify job executor:
    TransactionListener transactionListener = new JobAddedNotification(getJobExecutor());
    getCommandContext().getTransactionContext().addTransactionListener(TransactionState.COMMITTED, transactionListener);
  }

  @Override
  public List<JobEntity> findNextJobsToExecute(Page page) {
    return jobDataManager.findNextJobsToExecute(page);
  }

  @Override
  public List<JobEntity> findJobsByLockOwner(String lockOwner, int start, int maxNrOfJobs) {
    return jobDataManager.findJobsByLockOwner(lockOwner, start, maxNrOfJobs);
  }

  @Override
  public List<JobEntity> findJobsByExecutionId(String executionId) {
    return jobDataManager.findJobsByExecutionId(executionId);
  }

  @Override
  public List<JobEntity> findExclusiveJobsToExecute(String processInstanceId) {
    return jobDataManager.findExclusiveJobsToExecute(processInstanceId);
  }

  @Override
  public List<Job> findJobsByQueryCriteria(JobQueryImpl jobQuery, Page page) {
    return jobDataManager.findJobsByQueryCriteria(jobQuery, page);
  }

  @Override
  public List<Job> findJobsByTypeAndProcessDefinitionIds(String jobHandlerType, List<String> processDefinitionIds) {
    return jobDataManager.findJobsByTypeAndProcessDefinitionIds(jobHandlerType, processDefinitionIds);
  }

  @Override
  public List<Job> findJobsByTypeAndProcessDefinitionKeyNoTenantId(String jobHandlerType, String processDefinitionKey) {
    return jobDataManager.findJobsByTypeAndProcessDefinitionKeyNoTenantId(jobHandlerType, processDefinitionKey);
  }

  @Override
  public List<Job> findJobsByTypeAndProcessDefinitionKeyAndTenantId(String jobHandlerType, String processDefinitionKey, String tenantId) {
    return jobDataManager.findJobsByTypeAndProcessDefinitionKeyAndTenantId(jobHandlerType, processDefinitionKey, tenantId);
  }

  @Override
  public List<Job> findJobsByTypeAndProcessDefinitionId(String jobHandlerType, String processDefinitionId) {
    return jobDataManager.findJobsByTypeAndProcessDefinitionId(jobHandlerType, processDefinitionId);
  }

  @Override
  public long findJobCountByQueryCriteria(JobQueryImpl jobQuery) {
    return jobDataManager.findJobCountByQueryCriteria(jobQuery);
  }

  @Override
  public void updateJobTenantIdForDeployment(String deploymentId, String newTenantId) {
    jobDataManager.updateJobTenantIdForDeployment(deploymentId, newTenantId);
  }

  @Override
  public void unacquireJob(String jobId) {
    jobDataManager.unacquireJob(jobId);
  }

  @Override
  public void delete(JobEntity jobEntity) {
    super.delete(jobEntity);

    deleteExceptionByteArrayRef(jobEntity);

    removeExecutionLink(jobEntity);

    // Send event
    if (getEventDispatcher().isEnabled()) {
      getEventDispatcher().dispatchEvent(ActivitiEventBuilder.createEntityEvent(ActivitiEventType.ENTITY_DELETED, this));
    }
  }

  /**
   * Removes the job's execution's reference to this job, iff the job has an associated execution.
   * Subclasses may override to provide custom implementations.
   */
  protected void removeExecutionLink(JobEntity jobEntity) {
    if (jobEntity.getExecutionId() != null) {
      ExecutionEntity execution = getExecutionEntityManager().findById(jobEntity.getExecutionId());
      execution.getJobs().remove(this);
    }
  }

  /**
   * Deletes a the byte array used to store the exception information.  Subclasses may override
   * to provide custom implementations.
   */
  protected void deleteExceptionByteArrayRef(JobEntity jobEntity) {
    ByteArrayRef exceptionByteArrayRef = jobEntity.getExceptionByteArrayRef();
    if (exceptionByteArrayRef != null) {
      exceptionByteArrayRef.delete();
    }
  }

  // Job Execution logic ////////////////////////////////////////////////////////////////////

  @Override
  public void execute(JobEntity jobEntity) {
    if (jobEntity instanceof MessageEntity) {
      getAsyncJobEntityManager().execute(jobEntity);
    } else if (jobEntity instanceof TimerEntity) {
      getTimerJobEntityManager().execute(jobEntity);
    }
  }

}
