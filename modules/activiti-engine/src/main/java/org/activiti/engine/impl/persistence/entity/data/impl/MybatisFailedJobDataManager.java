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

import org.activiti.engine.impl.JobQueryImpl;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.persistence.CachedEntityMatcher;
import org.activiti.engine.impl.persistence.entity.ExecutableJobEntity;
import org.activiti.engine.impl.persistence.entity.ExecutableMessageJobEntity;
import org.activiti.engine.impl.persistence.entity.ExecutableMessageJobEntityImpl;
import org.activiti.engine.impl.persistence.entity.ExecutableTimerJobEntity;
import org.activiti.engine.impl.persistence.entity.ExecutableTimerJobEntityImpl;
import org.activiti.engine.impl.persistence.entity.FailedJobEntity;
import org.activiti.engine.impl.persistence.entity.FailedJobEntityImpl;
import org.activiti.engine.impl.persistence.entity.FailedMessageJobEntityImpl;
import org.activiti.engine.impl.persistence.entity.FailedTimerJobEntityImpl;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.activiti.engine.impl.persistence.entity.data.AbstractDataManager;
import org.activiti.engine.impl.persistence.entity.data.FailedJobDataManager;
import org.activiti.engine.runtime.Job;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vasile Dirla
 */
public class MybatisFailedJobDataManager extends AbstractDataManager<FailedJobEntity> implements FailedJobDataManager {

  public MybatisFailedJobDataManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
    super(processEngineConfiguration);
  }

  protected static List<Class<? extends FailedJobEntity>> ENTITY_SUBCLASSES = new ArrayList<Class<? extends FailedJobEntity>>();

  static {
    ENTITY_SUBCLASSES.add(FailedTimerJobEntityImpl.class);
    ENTITY_SUBCLASSES.add(FailedMessageJobEntityImpl.class);
  }

  @Override
  public Class<? extends FailedJobEntity> getManagedEntityClass() {
    return FailedJobEntityImpl.class;
  }

  @Override
  public List<Class<? extends FailedJobEntity>> getManagedEntitySubClasses() {
    return ENTITY_SUBCLASSES;
  }

  @Override
  public ExecutableMessageJobEntity createMessage() {
    return new ExecutableMessageJobEntityImpl();
  }

  @Override
  public ExecutableTimerJobEntity createTimer() {
    return new ExecutableTimerJobEntityImpl();
  }

  @Override
  public FailedJobEntity create() {
    // Superclass cannot be created
    throw new UnsupportedOperationException();
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<JobEntity> findNextJobsToExecute(Page page) {
    Date now = getClock().getCurrentTime();
    return getDbSqlSession().selectList("selectNextJobsToExecute", now, page);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<JobEntity> findNextTimerJobsToExecute(Page page) {
    Date now = getClock().getCurrentTime();
    return getDbSqlSession().selectList("selectNextTimerJobsToExecute", now, page);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<ExecutableJobEntity> findExecutableJobsDueToExecute(Page page) {
    Date now = getClock().getCurrentTime();
    return getDbSqlSession().selectList("selectExecutableJobsDueToExecute", now, page);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<JobEntity> findJobsByLockOwner(String lockOwner, int start, int maxNrOfJobs) {
    return getDbSqlSession().selectList("selectJobsByLockOwner", lockOwner, start, maxNrOfJobs);
  }

  @Override
  public List<FailedJobEntity> findJobsByExecutionId(final String executionId) {
    return getList("selectFailedJobsByExecutionId", executionId, new CachedEntityMatcher<FailedJobEntity>() {

      @Override
      public boolean isRetained(FailedJobEntity jobEntity) {
        return jobEntity.getExecutionId() != null && jobEntity.getExecutionId().equals(executionId);
      }
    }, true);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<JobEntity> findExclusiveJobsToExecute(String processInstanceId) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("pid", processInstanceId);
    params.put("now", getClock().getCurrentTime());
    return getDbSqlSession().selectList("selectExclusiveJobsToExecute", params);
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
    final String query = "selectFailedJobByQueryCriteria";
    return getDbSqlSession().selectList(query, jobQuery, page);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Job> findJobsByTypeAndProcessDefinitionIds(String jobHandlerType, List<String> processDefinitionIds) {
    Map<String, Object> params = new HashMap<String, Object>(2);
    params.put("handlerType", jobHandlerType);

    if (processDefinitionIds != null && processDefinitionIds.size() > 0) {
      params.put("processDefinitionIds", processDefinitionIds);
    }
    return getDbSqlSession().selectList("selectJobsByTypeAndProcessDefinitionIds", params);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Job> findJobsByTypeAndProcessDefinitionKeyNoTenantId(String jobHandlerType, String processDefinitionKey) {
    Map<String, String> params = new HashMap<String, String>(2);
    params.put("handlerType", jobHandlerType);
    params.put("processDefinitionKey", processDefinitionKey);
    return getDbSqlSession().selectList("selectJobByTypeAndProcessDefinitionKeyNoTenantId", params);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Job> findJobsByTypeAndProcessDefinitionKeyAndTenantId(String jobHandlerType, String processDefinitionKey, String tenantId) {
    Map<String, String> params = new HashMap<String, String>(3);
    params.put("handlerType", jobHandlerType);
    params.put("processDefinitionKey", processDefinitionKey);
    params.put("tenantId", tenantId);
    return getDbSqlSession().selectList("selectJobByTypeAndProcessDefinitionKeyAndTenantId", params);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Job> findJobsByTypeAndProcessDefinitionId(String jobHandlerType, String processDefinitionId) {
    Map<String, String> params = new HashMap<String, String>(2);
    params.put("handlerType", jobHandlerType);
    params.put("processDefinitionId", processDefinitionId);
    return getDbSqlSession().selectList("selectFailedJobByTypeAndProcessDefinitionId", params);
  }

  @Override
  public long findJobCountByQueryCriteria(JobQueryImpl jobQuery) {
    return (Long) getDbSqlSession().selectOne("selectFailedJobCountByQueryCriteria", jobQuery);
  }

  @Override
  public void updateJobTenantIdForDeployment(String deploymentId, String newTenantId) {
    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("deploymentId", deploymentId);
    params.put("tenantId", newTenantId);
    getDbSqlSession().update("updateJobTenantIdForDeployment", params);
  }

  @Override
  public void unacquireJob(String jobId) {
    Map<String, Object> params = new HashMap<String, Object>(2);
    params.put("id", jobId);
    params.put("dueDate", new Date(getProcessEngineConfiguration().getClock().getCurrentTime().getTime()));
    getDbSqlSession().update("unacquireJob", params);
  }

  @Override
  public int moveTimerJobsToMainQueue() {
    Date now = getClock().getCurrentTime();
    return getDbSqlSession().update("moveTimerJobsToMainQueue", now);
  }

  @Override
  public List<JobEntity> selectTimerJobsToDueDate() {
    Date now = getClock().getCurrentTime();
    return getDbSqlSession().selectList("selectTimerJobsToDueDate", now);
  }
  @Override
  public List<FailedJobEntity> selectRetriableJobs(Page page) {
    return getDbSqlSession().selectList("selectFailedRetriableJobs");
  }

}
