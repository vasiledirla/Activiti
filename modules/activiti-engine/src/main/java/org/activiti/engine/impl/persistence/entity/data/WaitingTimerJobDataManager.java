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
import org.activiti.engine.impl.persistence.entity.FailedJobEntity;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.activiti.engine.impl.persistence.entity.WaitingTimerJobEntity;
import org.activiti.engine.runtime.Job;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Vasile Dirla
 */
public interface WaitingTimerJobDataManager extends JobDataManager<WaitingTimerJobEntity> {

  WaitingTimerJobEntity createTimer();

  List<Job> findJobsByQueryCriteria(JobQueryImpl jobQuery, Page page);

  long findJobCountByQueryCriteria(JobQueryImpl jobQuery);

  List<WaitingTimerJobEntity> selectTimerJobsToDueDate();

  List<Job> findJobsByTypeAndProcessDefinitionId(String jobHandlerType, String processDefinitionId);

  Collection<WaitingTimerJobEntity> findJobsByExecutionId(String executionId);

  List<WaitingTimerJobEntity> findUnlockedTimersByDuedate(Date duedate, Page page);
}
