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
public class FailedTimerJobEntityImpl extends FailedJobEntityImpl implements FailedTimerJobEntity {

  protected int maxIterations;
  protected Date endDate;

  public FailedTimerJobEntityImpl(LockedTimerJobEntity jobEntity) {
    super(jobEntity);
    setEndDate(jobEntity.getEndDate());
    setMaxIterations(jobEntity.getMaxIterations());
  }

  public FailedTimerJobEntityImpl() {
    this(new LockedTimerJobEntityImpl());
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
