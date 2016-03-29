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
package org.activiti.engine.impl.persistence.entity.data;

import org.activiti.engine.impl.JobQueryImpl;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.persistence.entity.ExecutableJobEntity;
import org.activiti.engine.impl.persistence.entity.ExecutableMessageJobEntity;
import org.activiti.engine.impl.persistence.entity.ExecutableTimerJobEntity;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.LockedJobEntity;
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.activiti.engine.runtime.Job;

import java.util.Date;
import java.util.List;

/**
 * @author Vasile Dirla
 */
public interface LockedJobDataManager extends DataManager<LockedJobEntity> {

  ExecutableTimerJobEntity createTimer();

  ExecutableMessageJobEntity createMessage();
  
  List<JobEntity> findNextJobsToExecute(Page page);

  List<JobEntity> findNextTimerJobsToExecute(Page page);

  List<ExecutableJobEntity> findExecutableJobsDueToExecute(Page page);

  List<JobEntity> findJobsByLockOwner(String lockOwner, int start, int maxNrOfJobs);

  List<LockedJobEntity> findJobsByExecutionId(final String executionId);

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

  List<JobEntity> selectTimerJobsToDueDate();

  List<LockedJobEntity> selectExpiredJobs(long maxLockDuration, Page page);
}
