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

import org.activiti.bpmn.model.Event;
import org.activiti.bpmn.model.EventDefinition;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.TimerEventDefinition;
import org.activiti.engine.impl.JobQueryImpl;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.calendar.BusinessCalendar;
import org.activiti.engine.impl.calendar.CycleBusinessCalendar;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.cfg.TransactionListener;
import org.activiti.engine.impl.cfg.TransactionState;
import org.activiti.engine.impl.jobexecutor.AsyncJobAddedNotification;
import org.activiti.engine.impl.jobexecutor.JobAddedNotification;
import org.activiti.engine.impl.persistence.entity.data.DataManager;
import org.activiti.engine.impl.persistence.entity.data.LockedJobDataManager;
import org.activiti.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author Vasile Dirla
 */
public class LockedJobEntityManagerImpl extends AbstractJobEntityManager<LockedJobEntity> implements LockedJobEntityManager {

  private static final Logger logger = LoggerFactory.getLogger(LockedJobEntityManagerImpl.class);

  protected LockedJobDataManager jobDataManager;

  public LockedJobEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, LockedJobDataManager jobDataManager) {
    super(processEngineConfiguration);
    this.jobDataManager = jobDataManager;
  }

  @Override
  protected DataManager<LockedJobEntity> getDataManager() {
    return jobDataManager;
  }

  @Override
  public ExecutableMessageJobEntity createMessage() {
    return jobDataManager.createMessage();
  }

  @Override
  public ExecutableTimerJobEntity createTimer() {
    return jobDataManager.createTimer();
  }

  @Override
  public ExecutableTimerJobEntity createTimer(TimerEntity te) {
    ExecutableTimerJobEntity newTimerEntity = createTimer();
    newTimerEntity.setJobHandlerConfiguration(te.getJobHandlerConfiguration());
    newTimerEntity.setJobHandlerType(te.getJobHandlerType());
    newTimerEntity.setExclusive(te.isExclusive());
    newTimerEntity.setRepeat(te.getRepeat());
    newTimerEntity.setRetries(te.getRetries());
    newTimerEntity.setEndDate(te.getEndDate());
    newTimerEntity.setExecutionId(te.getExecutionId());
    newTimerEntity.setProcessInstanceId(te.getProcessInstanceId());
    newTimerEntity.setProcessDefinitionId(te.getProcessDefinitionId());

    // Inherit tenant
    newTimerEntity.setTenantId(te.getTenantId());
    newTimerEntity.setJobType("timer");
    return newTimerEntity;
  }

  @Override
  public void insert(LockedJobEntity jobEntity, boolean fireCreateEvent) {

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
  public void retryAsyncJob(LockedJobEntity job) {
    try {

      // If a job has to be retried, we wait for a certain amount of time,
      // otherwise the job will be continuously be retried without delay (and thus seriously stressing the database).
      Thread.sleep(getAsyncExecutor().getRetryWaitTimeInMillis());

    } catch (InterruptedException e) {
    }
    getAsyncExecutor().executeAsyncJob(job);
  }

  protected void hintAsyncExecutor(LockedJobEntity job) {

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
  public List<JobEntity> findNextTimerJobsToExecute(Page page) {
    return jobDataManager.findNextTimerJobsToExecute(page);
  }

  @Override
  public List<ExecutableJobEntity> findExecutableJobsDueToExecute(Page page) {
    return jobDataManager.findExecutableJobsDueToExecute(page);
  }

  @Override
  public List<JobEntity> findJobsByLockOwner(String lockOwner, int start, int maxNrOfJobs) {
    return jobDataManager.findJobsByLockOwner(lockOwner, start, maxNrOfJobs);
  }

  @Override
  public List<LockedJobEntity> findJobsByExecutionId(String executionId) {
    return jobDataManager.findJobsByExecutionId(executionId);
  }

  @Override
  public List<JobEntity> findExclusiveJobsToExecute(String processInstanceId) {
    return jobDataManager.findExclusiveJobsToExecute(processInstanceId);
  }

  @Override
  public List<TimerEntity> findUnlockedTimersByDuedate(Date duedate, Page page) {
    return jobDataManager.findUnlockedTimersByDuedate(duedate, page);
  }

  @Override
  public List<TimerEntity> findTimersByExecutionId(String executionId) {
    return jobDataManager.findTimersByExecutionId(executionId);
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
  public int moveTimerJobsToMainQueue() {
    return jobDataManager.moveTimerJobsToMainQueue();
  }

  @Override
  public List<JobEntity> selectTimerJobsToDueDate() {
    return jobDataManager.selectTimerJobsToDueDate();
  }
  @Override
  public List<LockedJobEntity> selectExpiredJobs(long maxLockDuration, Page page) {
    return jobDataManager.selectExpiredJobs(maxLockDuration, page);
  }

  protected int getMaxIterations(org.activiti.bpmn.model.Process process, String activityId) {
    FlowElement flowElement = process.getFlowElement(activityId, true);
    if (flowElement != null) {
      if (flowElement instanceof Event) {

        Event event = (Event) flowElement;
        List<EventDefinition> eventDefinitions = event.getEventDefinitions();

        if (eventDefinitions != null) {

          for (EventDefinition eventDefinition : eventDefinitions) {
            if (eventDefinition instanceof TimerEventDefinition) {
              TimerEventDefinition timerEventDefinition = (TimerEventDefinition) eventDefinition;
              if (timerEventDefinition.getTimeCycle() != null) {
                return calculateMaxIterationsValue(timerEventDefinition.getTimeCycle());
              }
            }
          }

        }

      }
    }
    return -1;
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

  public LockedJobDataManager getJobDataManager() {
    return jobDataManager;
  }

  public void setJobDataManager(LockedJobDataManager jobDataManager) {
    this.jobDataManager = jobDataManager;
  }

}
