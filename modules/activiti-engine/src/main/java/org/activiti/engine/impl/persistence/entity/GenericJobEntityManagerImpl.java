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
import org.activiti.engine.impl.persistence.entity.data.DataManager;
import org.activiti.engine.impl.persistence.entity.data.JobDataManager;
import org.activiti.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Vasile Dirla
 */
public abstract class GenericJobEntityManagerImpl extends AbstractEntityManager<JobEntity> implements GenericJobEntityManager {

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
    getAsyncExecutor().executeJob(job);
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

}
