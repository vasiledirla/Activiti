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
 */
public interface ExecutableJobEntityManager extends JobEntityManager<ExecutableJobEntity> {
  
  ExecutableMessageJobEntity createMessage();

  void execute(LockedJobEntity jobEntity);
  
  void send(ExecutableMessageJobEntity message);

  void schedule(ExecutableTimerJobEntity timer);

  void schedule(ExecutableTimerJobEntity timer, Boolean fireEvents);

  void retryAsyncJob(LockedJobEntity job);
  

  List<ExecutableJobEntity> findNextJobsToExecute(Page page);

  List<JobEntity> findNextTimerJobsToExecute(Page page);

  List<ExecutableJobEntity> findExecutableJobsDueToExecute(Page page);

  List<JobEntity> findJobsByLockOwner(String lockOwner, int start, int maxNrOfJobs);

  List<ExecutableJobEntity> findJobsByExecutionId(String executionId);

  List<ExecutableJobEntity> findExclusiveJobsToExecute(String processInstanceId);

  List<TimerEntity> findTimersByExecutionId(String executionId);

  List<Job> findJobsByQueryCriteria(JobQueryImpl jobQuery, Page page);

  List<Job> findJobsByTypeAndProcessDefinitionIds(String jobHandlerType, List<String> processDefinitionIds);

  List<Job> findJobsByTypeAndProcessDefinitionId(String jobHandlerType, String processDefinitionId);

  long findJobCountByQueryCriteria(JobQueryImpl jobQuery);

  LockedJobEntity lockJob(ExecutableJobEntity job, String lockOwner, Date time);

}