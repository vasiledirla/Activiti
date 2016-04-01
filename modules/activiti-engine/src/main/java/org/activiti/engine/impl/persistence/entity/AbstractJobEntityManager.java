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
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.persistence.entity.data.JobDataManager;
import org.activiti.engine.runtime.Job;

import java.util.List;

public abstract class AbstractJobEntityManager<T extends JobEntity> extends AbstractEntityManager<T> implements JobEntityManager<T> {

  public AbstractJobEntityManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
    super(processEngineConfiguration);
  }

  @Override
  public void delete(T jobEntity) {
    super.delete(jobEntity);

    deleteExceptionByteArrayRef(jobEntity);

    removeExecutionLink(jobEntity);

    // Send event
    if (getEventDispatcher().isEnabled()) {
      getEventDispatcher().dispatchEvent(ActivitiEventBuilder.createEntityEvent(ActivitiEventType.ENTITY_DELETED, this));
    }
  }

  @Override
  public void delete(T entity, boolean fireDeleteEvent) {
    getDataManager().delete(entity);

    deleteExceptionByteArrayRef(entity);

    removeExecutionLink(entity);

    if (fireDeleteEvent && getEventDispatcher().isEnabled()) {
      getEventDispatcher().dispatchEvent(ActivitiEventBuilder.createEntityEvent(ActivitiEventType.ENTITY_DELETED, entity));
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

  @Override
  public List<Job> findJobsByTypeAndProcessDefinitionKeyAndTenantId(String jobHandlerType, String processDefinitionKey, String tenantId) {
    return getDataManager().findJobsByTypeAndProcessDefinitionKeyAndTenantId(jobHandlerType, processDefinitionKey, tenantId);
  }

  @Override
  public List<Job> findJobsByTypeAndProcessDefinitionKeyNoTenantId(String jobHandlerType, String processDefinitionKey) {
    return getDataManager().findJobsByTypeAndProcessDefinitionKeyNoTenantId(jobHandlerType, processDefinitionKey);
  }

  @Override
  public void updateJobTenantIdForDeployment(String deploymentId, String newTenantId) {
    getDataManager().updateJobTenantIdForDeployment(deploymentId, newTenantId);
  }

  @Override
  protected abstract JobDataManager<T> getDataManager();

}
