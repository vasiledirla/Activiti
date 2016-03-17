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
import java.util.Map;

/**
 * @author Vasile Dirla
 */
public abstract class LockedJobEntityImpl extends JobEntityImpl implements LockedJobEntity {

  private static final long serialVersionUID = 1L;

  protected Date lockExpirationTime;

  protected String lockOwner;
  public LockedJobEntityImpl(JobEntity jobEntity, Date time, String lockOwner) {
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

    setLockOwner(lockOwner);
    setLockExpirationTime(time);
  }

  @Override
  public int getRevisionNext() {
    return revision;
  }
  public String getLockOwner() {
    return lockOwner;
  }

  public void setLockOwner(String claimedBy) {
    this.lockOwner = claimedBy;
  }

  public Date getLockExpirationTime() {
    return lockExpirationTime;
  }

  public void setLockExpirationTime(Date claimedUntil) {
    this.lockExpirationTime = claimedUntil;
  }

  public Object getPersistentState() {
    Map<String, Object> persistentState = (Map<String, Object>) super.getPersistentState();

    persistentState.put("lockOwner", lockOwner);
    persistentState.put("lockExpirationTime", lockExpirationTime);

    return persistentState;
  }

}
