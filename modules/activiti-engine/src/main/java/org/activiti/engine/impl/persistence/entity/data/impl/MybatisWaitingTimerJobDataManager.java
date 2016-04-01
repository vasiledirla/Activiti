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
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.activiti.engine.impl.persistence.entity.WaitingTimerJobEntity;
import org.activiti.engine.impl.persistence.entity.WaitingTimerJobEntityImpl;
import org.activiti.engine.impl.persistence.entity.data.AbstractDataManager;
import org.activiti.engine.impl.persistence.entity.data.WaitingTimerJobDataManager;
import org.activiti.engine.runtime.Job;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vasile Dirla
 */
public class MybatisWaitingTimerJobDataManager extends AbstractDataManager<WaitingTimerJobEntity> implements WaitingTimerJobDataManager {

  public MybatisWaitingTimerJobDataManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
    super(processEngineConfiguration);
  }

  protected static List<Class<? extends WaitingTimerJobEntity>> ENTITY_SUBCLASSES = new ArrayList<Class<? extends WaitingTimerJobEntity>>();

  static {
    ENTITY_SUBCLASSES.add(WaitingTimerJobEntityImpl.class);
  }

  @Override
  public Class<? extends WaitingTimerJobEntity> getManagedEntityClass() {
    return WaitingTimerJobEntityImpl.class;
  }

  @Override
  public List<Class<? extends WaitingTimerJobEntity>> getManagedEntitySubClasses() {
    return ENTITY_SUBCLASSES;
  }

  @Override
  public WaitingTimerJobEntity createTimer() {
    return new WaitingTimerJobEntityImpl();
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Job> findJobsByQueryCriteria(JobQueryImpl jobQuery, Page page) {
    String query = "selectWaitingTimerJobByQueryCriteria";
    return getDbSqlSession().selectList(query, jobQuery, page);
  }

  @Override
  public long findJobCountByQueryCriteria(JobQueryImpl jobQuery) {
    return (Long) getDbSqlSession().selectOne("selectWaitingTimerJobCountByQueryCriteria", jobQuery);
  }

  @Override
  public List<WaitingTimerJobEntity> selectTimerJobsToDueDate() {
    Date now = getClock().getCurrentTime();
    return getDbSqlSession().selectList("selectTimerJobsToDueDate", now);
  }
  @Override
  public List<Job> findJobsByTypeAndProcessDefinitionId(String jobHandlerType, String processDefinitionId) {
    Map<String, String> params = new HashMap<String, String>(2);
    params.put("handlerType", jobHandlerType);
    params.put("processDefinitionId", processDefinitionId);
    return getDbSqlSession().selectList("selectWaitingTimerJobByTypeAndProcessDefinitionId", params);

  }

  @Override
  public Collection<WaitingTimerJobEntity> findJobsByExecutionId(final String executionId) {
    return getList("selectWaitingTimerJobsByExecutionId", executionId, new CachedEntityMatcher<WaitingTimerJobEntity>() {

      @Override
      public boolean isRetained(WaitingTimerJobEntity jobEntity) {
        return jobEntity.getExecutionId() != null && jobEntity.getExecutionId().equals(executionId);
      }
    }, true);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<WaitingTimerJobEntity> findUnlockedTimersByDuedate(Date duedate, Page page) {
    final String query = "selectUnlockedTimersByDuedate";
    return getDbSqlSession().selectList(query, duedate, page);
  }

  @Override
  public WaitingTimerJobEntity create() {
    return createTimer();
  }


  @Override
  @SuppressWarnings("unchecked")
  public List<Job> findJobsByTypeAndProcessDefinitionKeyNoTenantId(String jobHandlerType, String processDefinitionKey) {
    Map<String, String> params = new HashMap<String, String>(2);
    params.put("handlerType", jobHandlerType);
    params.put("processDefinitionKey", processDefinitionKey);
    return getDbSqlSession().selectList("selectWaitingTimerJobByTypeAndProcessDefinitionKeyNoTenantId", params);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Job> findJobsByTypeAndProcessDefinitionKeyAndTenantId(String jobHandlerType, String processDefinitionKey, String tenantId) {
    Map<String, String> params = new HashMap<String, String>(3);
    params.put("handlerType", jobHandlerType);
    params.put("processDefinitionKey", processDefinitionKey);
    params.put("tenantId", tenantId);
    return getDbSqlSession().selectList("selectWaitingTimerJobByTypeAndProcessDefinitionKeyAndTenantId", params);
  }

  @Override
  public void updateJobTenantIdForDeployment(String deploymentId, String newTenantId) {
    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("deploymentId", deploymentId);
    params.put("tenantId", newTenantId);
    getDbSqlSession().update("updateWaitingTimerJobTenantIdForDeployment", params);
  }

}
