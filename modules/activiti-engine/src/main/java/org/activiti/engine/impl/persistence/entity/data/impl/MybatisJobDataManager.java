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
package org.activiti.engine.impl.persistence.entity.data.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.impl.JobQueryImpl;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.persistence.CachedEntityMatcher;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.JobEntityImpl;
import org.activiti.engine.impl.persistence.entity.MessageEntity;
import org.activiti.engine.impl.persistence.entity.MessageEntityImpl;
import org.activiti.engine.impl.persistence.entity.SuspensionState;
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.activiti.engine.impl.persistence.entity.TimerEntityImpl;
import org.activiti.engine.impl.persistence.entity.data.AbstractDataManager;
import org.activiti.engine.impl.persistence.entity.data.JobDataManager;
import org.activiti.engine.runtime.Job;

/**
 * @author Joram Barrez
 */
public class MybatisJobDataManager extends AbstractDataManager<JobEntity> implements JobDataManager {
  
  public MybatisJobDataManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
    super(processEngineConfiguration);
  }

  protected static List<Class<? extends JobEntity>> ENTITY_SUBCLASSES = new ArrayList<Class<? extends JobEntity>>();
  
  static {
    ENTITY_SUBCLASSES.add(TimerEntityImpl.class);
    ENTITY_SUBCLASSES.add(MessageEntityImpl.class);
  }
  
  @Override
  public Class<? extends JobEntity> getManagedEntityClass() {
    return JobEntityImpl.class;
  }
  
  @Override
  public List<Class<? extends JobEntity>> getManagedEntitySubClasses() {
    return ENTITY_SUBCLASSES;
  }
  
  @Override
  public MessageEntity createMessage() {
    return new MessageEntityImpl();
  }
  
  @Override
  public TimerEntity createTimer() {
    return new TimerEntityImpl();
  }
  
  @Override
  public JobEntity create() {
    // Superclass cannot be created
    throw new UnsupportedOperationException();
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public List<JobEntity> findNextAsyncJobsToExecute(Page page) {
    Date now = getClock().getCurrentTime();
    return getDbSqlSession().selectList("selectNextAsyncJobsToExecute", now, page);
  }

  @Override
  @SuppressWarnings("unchecked")
  public JobEntity selectAsyncJob(String jobIdentifier) {
    return (JobEntity) getDbSqlSession().selectOne("selectAsyncJob", jobIdentifier);
  }
  @Override

  @SuppressWarnings("unchecked")
  public JobEntity selectTimerJob(String jobIdentifier) {
    return (JobEntity) getDbSqlSession().selectOne("selectTimerJob", jobIdentifier);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<JobEntity> findNextTimerJobsToExecute(Page page) {
    Date now = getClock().getCurrentTime();
    return getDbSqlSession().selectList("selectNextTimerJobsToExecute", now, page);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<JobEntity> findAsyncJobsDueToExecute(Page page) {
    Date now = getClock().getCurrentTime();
    return getDbSqlSession().selectList("selectAsyncJobsDueToExecute", now, page);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<JobEntity> findAsyncJobsByLockOwner(String lockOwner, int start, int maxNrOfJobs) {
    return getDbSqlSession().selectList("selectAsyncJobsByLockOwner", lockOwner, start, maxNrOfJobs);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<JobEntity> findTimerJobsByLockOwner(String lockOwner, int start, int maxNrOfJobs) {
    return getDbSqlSession().selectList("selectTimerJobsByLockOwner", lockOwner, start, maxNrOfJobs);
  }

  @Override
  public List<JobEntity> findTimerJobsByExecutionId(final String executionId) {
    return getList("selectTimerJobsByExecutionId", executionId, new CachedEntityMatcher<JobEntity>() {
      @Override
      public boolean isRetained(JobEntity jobEntity) {
        return jobEntity.getExecutionId() != null && jobEntity.getExecutionId().equals(executionId);
      }
    }, true);
  }

  @Override
  public List<JobEntity> findAsyncJobsByExecutionId(final String executionId) {
    return getList("selectAsyncJobsByExecutionId", executionId, new CachedEntityMatcher<JobEntity>() {
      @Override
      public boolean isRetained(JobEntity jobEntity) {
        return jobEntity.getExecutionId() != null && jobEntity.getExecutionId().equals(executionId);
      }
    }, true);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<JobEntity> findExclusiveTimerJobsToExecute(String processInstanceId) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("pid", processInstanceId);
    params.put("now", getClock().getCurrentTime());
    return getDbSqlSession().selectList("selectExclusiveTimerJobsToExecute", params);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<JobEntity> findExclusiveAsyncJobsToExecute(String processInstanceId) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("pid", processInstanceId);
    params.put("now", getClock().getCurrentTime());
    return getDbSqlSession().selectList("selectExclusiveAsyncJobsToExecute", params);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<TimerEntity> findUnlockedTimersByDuedate(Date duedate, Page page) {
    final String query = "selectUnlockedTimersByDuedate";
    return getDbSqlSession().selectList(query, duedate, page);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<TimerEntity> findTimersByExecutionId(String executionId) {
    return getDbSqlSession().selectList("selectTimersByExecutionId", executionId);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Job> findJobsByQueryCriteria(JobQueryImpl jobQuery, Page page) {
    return getDbSqlSession().selectList("selectJobByQueryCriteria", jobQuery, page);
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public List<Job> findAsyncJobsByTypeAndProcessDefinitionIds(String jobHandlerType, List<String> processDefinitionIds) {
    Map<String, Object> params = new HashMap<String, Object>(2);
    params.put("handlerType", jobHandlerType);
    
    if (processDefinitionIds != null && processDefinitionIds.size() > 0) {
      params.put("processDefinitionIds", processDefinitionIds);
    }
    return getDbSqlSession().selectList("selectAsyncJobsByTypeAndProcessDefinitionIds", params);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Job> findTimerJobsByTypeAndProcessDefinitionIds(String jobHandlerType, List<String> processDefinitionIds) {
    Map<String, Object> params = new HashMap<String, Object>(2);
    params.put("handlerType", jobHandlerType);

    if (processDefinitionIds != null && processDefinitionIds.size() > 0) {
      params.put("processDefinitionIds", processDefinitionIds);
    }
    return getDbSqlSession().selectList("selectTimerJobsByTypeAndProcessDefinitionIds", params);
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public List<Job> findAsyncJobsByTypeAndProcessDefinitionKeyNoTenantId(String jobHandlerType, String processDefinitionKey) {
     Map<String, String> params = new HashMap<String, String>(2);
     params.put("handlerType", jobHandlerType);
     params.put("processDefinitionKey", processDefinitionKey);
     return getDbSqlSession().selectList("selectAsyncJobByTypeAndProcessDefinitionKeyNoTenantId", params);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Job> findTimerJobsByTypeAndProcessDefinitionKeyNoTenantId(String jobHandlerType, String processDefinitionKey) {
     Map<String, String> params = new HashMap<String, String>(2);
     params.put("handlerType", jobHandlerType);
     params.put("processDefinitionKey", processDefinitionKey);
     return getDbSqlSession().selectList("selectTimerJobByTypeAndProcessDefinitionKeyNoTenantId", params);
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public List<Job> findAsyncJobsByTypeAndProcessDefinitionKeyAndTenantId(String jobHandlerType, String processDefinitionKey, String tenantId) {
     Map<String, String> params = new HashMap<String, String>(3);
     params.put("handlerType", jobHandlerType);
     params.put("processDefinitionKey", processDefinitionKey);
     params.put("tenantId", tenantId);
     return getDbSqlSession().selectList("selectAsyncJobByTypeAndProcessDefinitionKeyAndTenantId", params);
  }

    @Override
  @SuppressWarnings("unchecked")
  public List<Job> findTimerJobsByTypeAndProcessDefinitionKeyAndTenantId(String jobHandlerType, String processDefinitionKey, String tenantId) {
     Map<String, String> params = new HashMap<String, String>(3);
     params.put("handlerType", jobHandlerType);
     params.put("processDefinitionKey", processDefinitionKey);
     params.put("tenantId", tenantId);
     return getDbSqlSession().selectList("selectTimerJobByTypeAndProcessDefinitionKeyAndTenantId", params);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Job> findAsyncJobsByTypeAndProcessDefinitionId(String jobHandlerType, String processDefinitionId) {
     Map<String, String> params = new HashMap<String, String>(2);
     params.put("handlerType", jobHandlerType);
     params.put("processDefinitionId", processDefinitionId);
     return getDbSqlSession().selectList("selectAsyncJobByTypeAndProcessDefinitionId", params);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Job> findTimerJobsByTypeAndProcessDefinitionId(String jobHandlerType, String processDefinitionId) {
     Map<String, String> params = new HashMap<String, String>(2);
     params.put("handlerType", jobHandlerType);
     params.put("processDefinitionId", processDefinitionId);
     return getDbSqlSession().selectList("selectTimerJobByTypeAndProcessDefinitionId", params);
  }

  @Override
  public long findJobCountByQueryCriteria(JobQueryImpl jobQuery) {
    return (Long) getDbSqlSession().selectOne("selectJobCountByQueryCriteria", jobQuery);
  }

  @Override
  public void updateAsyncJobTenantIdForDeployment(String deploymentId, String newTenantId) {
    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("deploymentId", deploymentId);
    params.put("tenantId", newTenantId);
    getDbSqlSession().update("updateAsyncJobTenantIdForDeployment", params);
  }

  @Override
  public void updateTimerJobTenantIdForDeployment(String deploymentId, String newTenantId) {
    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("deploymentId", deploymentId);
    params.put("tenantId", newTenantId);
    getDbSqlSession().update("updateTimerJobTenantIdForDeployment", params);
  }
  
  @Override
  public void unacquireAsyncJob(String jobId) {
    Map<String, Object> params = new HashMap<String, Object>(2);
    params.put("id", jobId);
    params.put("dueDate", new Date(getProcessEngineConfiguration().getClock().getCurrentTime().getTime()));
    getDbSqlSession().update("unacquireAsyncJob", params);
  }

  @Override
  public void unacquireTimerJob(String jobId) {
    Map<String, Object> params = new HashMap<String, Object>(2);
    params.put("id", jobId);
    params.put("dueDate", new Date(getProcessEngineConfiguration().getClock().getCurrentTime().getTime()));
    getDbSqlSession().update("unacquireTimerJob", params);
  }


  @Override
  public void updateSuspensionStateForAsyncJobsByExecution(String executionId, SuspensionState newState) {
    Map<String, Object> params = new HashMap<String, Object>(2);
    params.put("pid", executionId);
    params.put("newState", newState.getStateCode());
    getDbSqlSession().update("updateSuspensionStateForAsyncJobsByExecution", params);
  }


  @Override
  public void updateSuspensionStateForTimerJobsByExecution(String executionId, SuspensionState newState) {
    Map<String, Object> params = new HashMap<String, Object>(2);
    params.put("pid", executionId);
    params.put("newState", newState.getStateCode());
    getDbSqlSession().update("updateSuspensionStateForTimerJobsByExecution", params);
  }

}
