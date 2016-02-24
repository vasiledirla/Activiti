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
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public interface TimerEntity extends JobEntity {

  String getRepeat();

  void setRepeat(String repeat);

  Date getEndDate();

  void setEndDate(Date endDate);

  int getMaxIterations();

  void setMaxIterations(int maxIterations);

  /**
   * Returns the date on which this job is supposed to be processed.
   */
  Date getDuedate();

  void setDuedate(Date duedate);

}
