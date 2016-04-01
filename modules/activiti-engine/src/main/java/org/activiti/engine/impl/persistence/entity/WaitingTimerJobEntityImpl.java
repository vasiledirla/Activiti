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

import java.util.Date;

/**
 * @author Vasile Dirla
 */
public class WaitingTimerJobEntityImpl extends JobEntityImpl implements WaitingTimerJobEntity {

  private static final long serialVersionUID = 1L;

  protected int maxIterations;
  protected Date endDate;

  public WaitingTimerJobEntityImpl(TimerEntity jobEntity) {
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

    setEndDate(jobEntity.getEndDate());
    setMaxIterations(jobEntity.getMaxIterations());
  }

  @Override
  public int getRevisionNext() {
    return revision;
  }

  public WaitingTimerJobEntityImpl() {
    this(new TimerEntityImpl());
  }

  @Override
  public Date getEndDate() {
    return endDate;
  }
  @Override
  public void setEndDate(Date endDate) {
    this.endDate = endDate;
  }
  @Override
  public int getMaxIterations() {
    return maxIterations;
  }
  @Override
  public void setMaxIterations(int maxIterations) {
    this.maxIterations = maxIterations;
  }
}
