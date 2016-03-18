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

import org.activiti.engine.impl.JobQueryImpl;
import org.activiti.engine.impl.Page;
import org.activiti.engine.runtime.Job;

import java.util.Date;
import java.util.List;

/**
 * @author Vasile Dirla
 */
public interface FailedJobEntityManager extends JobEntityManager<FailedJobEntity> {

  ExecutableTimerJobEntity createTimer();

  ExecutableTimerJobEntity createTimer(TimerEntity timerEntity);

  ExecutableMessageJobEntity createMessage();

  void retryAsyncJob(LockedJobEntity job);

  List<JobEntity> findNextJobsToExecute(Page page);

  List<JobEntity> findNextTimerJobsToExecute(Page page);

  List<ExecutableJobEntity> findExecutableJobsDueToExecute(Page page);

  List<JobEntity> findJobsByLockOwner(String lockOwner, int start, int maxNrOfJobs);

  List<FailedJobEntity> findJobsByExecutionId(String executionId);

  List<JobEntity> findExclusiveJobsToExecute(String processInstanceId);

  List<TimerEntity> findUnlockedTimersByDuedate(Date duedate, Page page);

  List<TimerEntity> findTimersByExecutionId(String executionId);

  List<Job> findJobsByQueryCriteria(JobQueryImpl jobQuery, Page page);

  List<Job> findJobsByTypeAndProcessDefinitionIds(String jobHandlerType, List<String> processDefinitionIds);

  List<Job> findJobsByTypeAndProcessDefinitionKeyNoTenantId(String jobHandlerType, String processDefinitionKey);

  List<Job> findJobsByTypeAndProcessDefinitionKeyAndTenantId(String jobHandlerType, String processDefinitionKey, String tenantId);

  List<Job> findJobsByTypeAndProcessDefinitionId(String jobHandlerType, String processDefinitionId);

  long findJobCountByQueryCriteria(JobQueryImpl jobQuery);

  void updateJobTenantIdForDeployment(String deploymentId, String newTenantId);

  int moveTimerJobsToMainQueue();

  List<JobEntity> selectTimerJobsToDueDate();

  List<FailedJobEntity> selectRetriableJobs(Page page);
}