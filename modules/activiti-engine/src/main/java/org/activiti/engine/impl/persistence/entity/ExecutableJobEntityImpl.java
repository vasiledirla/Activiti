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

/**
 * @author Vasile Dirla
 */
public abstract class ExecutableJobEntityImpl extends JobEntityImpl implements ExecutableJobEntity {

  private static final long serialVersionUID = 1L;

  public ExecutableJobEntityImpl(JobEntity jobEntity) {
    setId(jobEntity.getId());
    setRepeat(jobEntity.getRepeat());
    setExceptionStacktrace(jobEntity.getExceptionStacktrace());
    setJobHandlerConfiguration(jobEntity.getJobHandlerConfiguration());
    setJobHandlerType(jobEntity.getJobHandlerType());
    setJobType(jobEntity.getJobType());
    setExceptionMessage(jobEntity.getExceptionMessage());
    setExecutionId(jobEntity.getExecutionId());
    setProcessDefinitionId(jobEntity.getProcessDefinitionId());
    setProcessInstanceId(jobEntity.getProcessInstanceId());
    setTenantId(jobEntity.getTenantId());
    setDuedate(jobEntity.getDuedate());
    setRetries(jobEntity.getRetries());
    setRevision(jobEntity.getRevision());
    setExclusive(jobEntity.isExclusive());

  }


  @Override
  public int getRevisionNext() {
    return revision;
  }

}
