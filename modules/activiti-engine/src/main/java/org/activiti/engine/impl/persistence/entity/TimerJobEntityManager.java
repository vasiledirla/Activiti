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
import java.util.List;

import org.activiti.engine.impl.JobQueryImpl;
import org.activiti.engine.impl.Page;
import org.activiti.engine.runtime.Job;

/**
 * @author Joram Barrez
 * @author Vasile Dirla
 */
public interface TimerJobEntityManager extends JobEntityManager {
  
  TimerEntity createTimer();
  
  TimerEntity createTimer(TimerEntity timerEntity);

  void schedule(TimerEntity timer);

  List<TimerEntity> findUnlockedTimersByDuedate(Date duedate, Page page);

  List<TimerEntity> findTimersByExecutionId(String executionId);

  List<JobEntity> findNextTimerJobsToExecute(Page page);

  void unacquireTimerJob(String jobId);
}