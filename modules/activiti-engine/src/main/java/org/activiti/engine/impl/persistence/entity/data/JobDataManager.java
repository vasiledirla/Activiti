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

import java.util.Date;
import java.util.List;

import org.activiti.engine.impl.JobQueryImpl;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.MessageEntity;
import org.activiti.engine.impl.persistence.entity.SuspensionState;
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.activiti.engine.runtime.Job;

/**
 * @author Joram Barrez
 */
public interface JobDataManager extends DataManager<JobEntity> {
  
  TimerEntity createTimer();
  
  MessageEntity createMessage();

  JobEntity selectAsyncJob(String id);

  JobEntity selectTimerJob(String id);

  List<JobEntity> findNextAsyncJobsToExecute(Page page);

  List<JobEntity> findNextTimerJobsToExecute(Page page);

  List<JobEntity> findAsyncJobsDueToExecute(Page page);

  List<JobEntity> findAsyncJobsByLockOwner(String lockOwner, int start, int maxNrOfJobs);

  List<JobEntity> findTimerJobsByLockOwner(String lockOwner, int start, int maxNrOfJobs);

  List<JobEntity> findAsyncJobsByExecutionId(final String executionId);

  List<JobEntity> findTimerJobsByExecutionId(final String executionId);

  List<JobEntity> findExclusiveAsyncJobsToExecute(String processInstanceId);

  List<JobEntity> findExclusiveTimerJobsToExecute(String processInstanceId);

  List<TimerEntity> findUnlockedTimersByDuedate(Date duedate, Page page);

  List<TimerEntity> findTimersByExecutionId(String executionId);

  List<Job> findJobsByQueryCriteria(JobQueryImpl jobQuery, Page page);
  
  List<Job> findAsyncJobsByTypeAndProcessDefinitionIds(String jobHandlerType, List<String> processDefinitionIds);

  List<Job> findTimerJobsByTypeAndProcessDefinitionIds(String jobHandlerType, List<String> processDefinitionIds);

  List<Job> findAsyncJobsByTypeAndProcessDefinitionKeyNoTenantId(String jobHandlerType, String processDefinitionKey);

  List<Job> findTimerJobsByTypeAndProcessDefinitionKeyNoTenantId(String jobHandlerType, String processDefinitionKey);

  List<Job> findAsyncJobsByTypeAndProcessDefinitionKeyAndTenantId(String jobHandlerType, String processDefinitionKey, String tenantId);

  List<Job> findTimerJobsByTypeAndProcessDefinitionKeyAndTenantId(String jobHandlerType, String processDefinitionKey, String tenantId);

  List<Job> findAsyncJobsByTypeAndProcessDefinitionId(String jobHandlerType, String processDefinitionId);

  List<Job> findTimerJobsByTypeAndProcessDefinitionId(String jobHandlerType, String processDefinitionId);

  long findJobCountByQueryCriteria(JobQueryImpl jobQuery);

  void updateAsyncJobTenantIdForDeployment(String deploymentId, String newTenantId);

  void updateTimerJobTenantIdForDeployment(String deploymentId, String newTenantId);

  void unacquireTimerJob(String jobId);

  void unacquireAsyncJob(String jobId);

  void updateSuspensionStateForAsyncJobsByExecution(String executionId, SuspensionState newState);

  void updateSuspensionStateForTimerJobsByExecution(String executionId, SuspensionState newState);
}
