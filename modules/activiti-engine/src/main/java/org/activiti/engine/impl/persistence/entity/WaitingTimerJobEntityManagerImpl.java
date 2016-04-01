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

import org.activiti.engine.impl.JobQueryImpl;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.persistence.entity.data.DataManager;
import org.activiti.engine.impl.persistence.entity.data.JobDataManager;
import org.activiti.engine.impl.persistence.entity.data.WaitingTimerJobDataManager;
import org.activiti.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Vasile Dirla
 */
public class WaitingTimerJobEntityManagerImpl extends AbstractJobEntityManager<WaitingTimerJobEntity> implements WaitingTimerJobEntityManager {

  private static final Logger logger = LoggerFactory.getLogger(WaitingTimerJobEntityManagerImpl.class);

  protected WaitingTimerJobDataManager jobDataManager;

  public WaitingTimerJobEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, WaitingTimerJobDataManager jobDataManager) {
    super(processEngineConfiguration);
    this.jobDataManager = jobDataManager;
  }

  @Override
  protected JobDataManager<WaitingTimerJobEntity> getDataManager() {
    return jobDataManager;
  }

  public WaitingTimerJobDataManager getJobDataManager() {
    return jobDataManager;
  }

  public void setJobDataManager(WaitingTimerJobDataManager jobDataManager) {
    this.jobDataManager = jobDataManager;
  }

  @Override
  public List<Job> findJobsByQueryCriteria(JobQueryImpl jobQuery, Page page) {
    return jobDataManager.findJobsByQueryCriteria(jobQuery, page);
  }
  @Override
  public long findJobCountByQueryCriteria(JobQueryImpl jobQuery) {
    return jobDataManager.findJobCountByQueryCriteria(jobQuery);
  }

  @Override
  public WaitingTimerJobEntity createTimer() {
    return jobDataManager.createTimer();
  }

  @Override
  public WaitingTimerJobEntity createTimer(TimerEntity te) {
    WaitingTimerJobEntity newTimerEntity = createTimer();
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
  public List<WaitingTimerJobEntity> selectTimerJobsToDueDate() {
    return jobDataManager.selectTimerJobsToDueDate();
  }

  @Override
  public List<Job> findJobsByTypeAndProcessDefinitionId(String jobHandlerType, String processDefinitionId) {
    return jobDataManager.findJobsByTypeAndProcessDefinitionId(jobHandlerType, processDefinitionId);
  }
  @Override
  public Collection<WaitingTimerJobEntity> findJobsByExecutionId(String id) {
    return jobDataManager.findJobsByExecutionId(id);
  }
  @Override
  public List<WaitingTimerJobEntity> findUnlockedTimersByDuedate(Date duedate, Page page) {
    return jobDataManager.findUnlockedTimersByDuedate(duedate, page);
  }

  @Override
  public void insert(WaitingTimerJobEntity jobEntity, boolean fireCreateEvent) {

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
  public void insert(WaitingTimerJobEntity jobEntity) {
    insert(jobEntity, true);
  }

}
