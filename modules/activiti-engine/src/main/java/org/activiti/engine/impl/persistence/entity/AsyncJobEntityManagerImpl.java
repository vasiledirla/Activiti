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
import org.activiti.engine.impl.calendar.BusinessCalendar;
import org.activiti.engine.impl.calendar.CycleBusinessCalendar;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.cfg.TransactionListener;
import org.activiti.engine.impl.cfg.TransactionState;
import org.activiti.engine.impl.db.JobQueryParameterObject;
import org.activiti.engine.impl.jobexecutor.AsyncJobAddedNotification;
import org.activiti.engine.impl.jobexecutor.JobAddedNotification;
import org.activiti.engine.impl.jobexecutor.JobHandler;
import org.activiti.engine.impl.persistence.entity.data.DataManager;
import org.activiti.engine.impl.persistence.entity.data.JobDataManager;
import org.activiti.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Vasile Dirla
 */
public class AsyncJobEntityManagerImpl extends AbstractEntityManager<JobEntity> implements AsyncJobEntityManager {

  private static final Logger logger = LoggerFactory.getLogger(AsyncJobEntityManagerImpl.class);

  protected JobDataManager jobDataManager;

  public AsyncJobEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, JobDataManager jobDataManager) {
    super(processEngineConfiguration);
    this.jobDataManager = jobDataManager;
  }

  @Override
  protected DataManager<JobEntity> getDataManager() {
    return jobDataManager;
  }

  @Override
  public MessageEntity createMessage() {
    return jobDataManager.createMessage();
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
      message.setDuedate(dueDate);
      message.setLockExpirationTime(null); // was set before, but to be quickly picked up needs to be set to null

    } else if (!processEngineConfiguration.isJobExecutorActivate()) {

      // If the async executor is disabled AND there is no old school job
      // executor, The job needs to be picked up as soon as possible. So the due date is now set to the current time
      message.setDuedate(processEngineConfiguration.getClock().getCurrentTime());
      message.setLockExpirationTime(null); // was set before, but to be quickly picked up needs to be set to null
    }

    insert(message);
    if (processEngineConfiguration.isAsyncExecutorEnabled()) {
      hintAsyncExecutor(message);
    } else {
      hintJobExecutor(message);
    }
  }

  @Override
  public void retryAsyncJob(JobEntity job) {
    try {

      // If a job has to be retried, we wait for a certain amount of time,
      // otherwise the job will be continuously be retried without delay (and thus seriously stressing the database).
      Thread.sleep(getAsyncExecutor().getRetryWaitTimeInMillis());

    } catch (InterruptedException e) {
    }
    getAsyncExecutor().executeJob(job);
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
  public List<JobEntity> findAsyncJobsDueToExecute(Page page) {
    return jobDataManager.findAsyncJobsDueToExecute(page);
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
  public void unacquireAsyncJob(String jobId) {
    jobDataManager.unacquireJob(Job.MESSAGE, jobId);
  }

  @Override
  public void unacquireJob(Job job) {
    if (job instanceof MessageEntity) {
      unacquireAsyncJob(job.getId());
    }
  }

  @Override
  public JobEntity findById(String jobId) {
    return jobDataManager.selectJob(new JobQueryParameterObject(jobId, Job.MESSAGE));
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

  protected int calculateMaxIterationsValue(String originalExpression) {
    int times = Integer.MAX_VALUE;
    List<String> expression = Arrays.asList(originalExpression.split("/"));
    if (expression.size() > 1 && expression.get(0).startsWith("R")) {
      times = Integer.MAX_VALUE;
      if (expression.get(0).length() > 1) {
        times = Integer.parseInt(expression.get(0).substring(1));
      }
    }
    return times;
  }

  protected int calculateRepeatValue(TimerEntity timerEntity) {
    int times = -1;
    List<String> expression = Arrays.asList(timerEntity.getRepeat().split("/"));
    if (expression.size() > 1 && expression.get(0).startsWith("R") && expression.get(0).length() > 1) {
      times = Integer.parseInt(expression.get(0).substring(1));
      if (times > 0) {
        times--;
      }
    }
    return times;
  }

  protected void setNewRepeat(TimerEntity timerEntity, int newRepeatValue) {
    List<String> expression = Arrays.asList(timerEntity.getRepeat().split("/"));
    expression = expression.subList(1, expression.size());
    StringBuilder repeatBuilder = new StringBuilder("R");
    repeatBuilder.append(newRepeatValue);
    for (String value : expression) {
      repeatBuilder.append("/");
      repeatBuilder.append(value);
    }
    timerEntity.setRepeat(repeatBuilder.toString());
  }

  protected boolean isValidTime(TimerEntity timerEntity, Date newTimerDate) {
    BusinessCalendar businessCalendar = getProcessEngineConfiguration().getBusinessCalendarManager().getBusinessCalendar(CycleBusinessCalendar.NAME);
    return businessCalendar.validateDuedate(timerEntity.getRepeat(), timerEntity.getMaxIterations(), timerEntity.getEndDate(), newTimerDate);
  }

  protected Date calculateNextTimer(TimerEntity timerEntity) {
    BusinessCalendar businessCalendar = getProcessEngineConfiguration().getBusinessCalendarManager().getBusinessCalendar(CycleBusinessCalendar.NAME);
    return businessCalendar.resolveDuedate(timerEntity.getRepeat(), timerEntity.getMaxIterations());
  }

  public JobDataManager getJobDataManager() {
    return jobDataManager;
  }

  public void setJobDataManager(JobDataManager jobDataManager) {
    this.jobDataManager = jobDataManager;
  }

}
