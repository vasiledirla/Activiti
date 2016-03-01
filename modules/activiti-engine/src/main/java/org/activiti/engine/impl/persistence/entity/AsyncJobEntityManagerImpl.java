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

import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.cfg.TransactionListener;
import org.activiti.engine.impl.cfg.TransactionState;
import org.activiti.engine.impl.jobexecutor.AsyncJobAddedNotification;
import org.activiti.engine.impl.jobexecutor.JobAddedNotification;
import org.activiti.engine.impl.jobexecutor.JobHandler;
import org.activiti.engine.impl.persistence.entity.data.JobDataManager;
import org.activiti.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Vasile Dirla
 */
public class AsyncJobEntityManagerImpl extends GenericJobEntityManagerImpl implements AsyncJobEntityManager {

  private static final Logger logger = LoggerFactory.getLogger(AsyncJobEntityManagerImpl.class);

  public AsyncJobEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, JobDataManager jobDataManager) {
    super(processEngineConfiguration, jobDataManager);
  }

  @Override
  public MessageEntity createMessage() {
    return jobDataManager.createMessage();
  }

  @Override
  public void send(MessageEntity message) {

    ProcessEngineConfigurationImpl processEngineConfiguration = getProcessEngineConfiguration();

    if (processEngineConfiguration.isAsyncExecutorEnabled()) {

      // If the async executor is enabled, we need to set the duedate of
      // the job to the current date + the default lock time.
      // This is cope with the case where the async job executor or the
      // process engine goes down
      // before executing the job. This way, other async job executors can
      // pick the job up after the max lock time.
      Date dueDate = new Date(getClock().getCurrentTime().getTime() + processEngineConfiguration.getAsyncExecutor().getAsyncJobLockTimeInMillis());
      //      message.setDuedate(dueDate);
      message.setLockExpirationTime(dueDate); // was set before, but to be quickly picked up needs to be set to null

    } else if (!processEngineConfiguration.isJobExecutorActivate()) {

      // If the async executor is disabled AND there is no old school job
      // executor, The job needs to be picked up as soon as possible. So the due date is now set to the current time
      //      message.setDuedate(processEngineConfiguration.getClock().getCurrentTime());
      message.setLockExpirationTime(
              processEngineConfiguration.getClock().getCurrentTime()); // was set before, but to be quickly picked up needs to be set to null
    }

    insert(message);
    if (processEngineConfiguration.isAsyncExecutorEnabled()) {
      hintAsyncExecutor(message);
    } else {
      hintJobExecutor(message);
    }
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
    return jobDataManager.findNextAsyncJobsToExecute(page);
  }

  @Override
  public List<JobEntity> findAsyncJobsDueToExecute(Page page) {
    return jobDataManager.findAsyncJobsDueToExecute(page);
  }

  @Override
  public List<JobEntity> findJobsByLockOwner(String lockOwner, int start, int maxNrOfJobs) {
    return jobDataManager.findAsyncJobsByLockOwner(lockOwner, start, maxNrOfJobs);
  }

  @Override
  public List<JobEntity> findJobsByExecutionId(String executionId) {
    return jobDataManager.findAsyncJobsByExecutionId(executionId);
  }

  @Override
  public List<JobEntity> findExclusiveJobsToExecute(String processInstanceId) {
    return jobDataManager.findExclusiveAsyncJobsToExecute(processInstanceId);
  }

  @Override
  public List<Job> findJobsByTypeAndProcessDefinitionIds(String jobHandlerType, List<String> processDefinitionIds) {
    return jobDataManager.findAsyncJobsByTypeAndProcessDefinitionIds(jobHandlerType, processDefinitionIds);
  }

  @Override
  public List<Job> findJobsByTypeAndProcessDefinitionKeyNoTenantId(String jobHandlerType, String processDefinitionKey) {
    return jobDataManager.findAsyncJobsByTypeAndProcessDefinitionKeyNoTenantId(jobHandlerType, processDefinitionKey);
  }

  @Override
  public List<Job> findJobsByTypeAndProcessDefinitionKeyAndTenantId(String jobHandlerType, String processDefinitionKey, String tenantId) {
    return jobDataManager.findAsyncJobsByTypeAndProcessDefinitionKeyAndTenantId(jobHandlerType, processDefinitionKey, tenantId);
  }

  @Override
  public List<Job> findJobsByTypeAndProcessDefinitionId(String jobHandlerType, String processDefinitionId) {
    return jobDataManager.findAsyncJobsByTypeAndProcessDefinitionId(jobHandlerType, processDefinitionId);
  }

  @Override
  public void updateJobTenantIdForDeployment(String deploymentId, String newTenantId) {
    jobDataManager.updateAsyncJobTenantIdForDeployment(deploymentId, newTenantId);
  }

  @Override
  public void unacquireAsyncJob(String jobId) {
    jobDataManager.unacquireAsyncJob(jobId);
  }

  @Override
  public void updateSuspensionStateForJobsByExecution(String executionId, SuspensionState newState){
    jobDataManager.updateSuspensionStateForAsyncJobsByExecution(executionId, newState);
  }

  @Override
  public void unacquireJob(Job job) {
    if (job instanceof MessageEntity) {
      unacquireAsyncJob(job.getId());
    }
  }

  @Override
  public JobEntity findById(String jobId) {
    return jobDataManager.selectAsyncJob(jobId);
  }

  // Job Execution logic ////////////////////////////////////////////////////////////////////

  @Override
  public void execute(JobEntity jobEntity) {
    if (jobEntity instanceof MessageEntity) {
      executeMessageJob(jobEntity);
    }
  }

  protected void executeJobHandler(JobEntity jobEntity) {

    ExecutionEntity execution = null;
    if (jobEntity.getExecutionId() != null) {
      execution = getExecutionEntityManager().findById(jobEntity.getExecutionId());
    }

    Map<String, JobHandler> jobHandlers = getProcessEngineConfiguration().getJobHandlers();
    JobHandler jobHandler = jobHandlers.get(jobEntity.getJobHandlerType());
    jobHandler.execute(jobEntity, jobEntity.getJobHandlerConfiguration(), execution, getCommandContext());
  }

  protected void executeMessageJob(JobEntity jobEntity) {
    executeJobHandler(jobEntity);
    delete(jobEntity);
  }

}
