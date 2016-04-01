package org.activiti.engine.impl.cmd;/* Licensed under the Apache License, Version 2.0 (the "License");
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

import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutableTimerJobEntity;
import org.activiti.engine.impl.persistence.entity.WaitingTimerJobEntity;

import java.util.List;

public class MoveTimerJobsDueDate implements Command<Integer> {

  @Override
  public Integer execute(CommandContext commandContext) {
    List<WaitingTimerJobEntity> timerJobs = commandContext.getWaitingTimerJobEntityManager().selectTimerJobsToDueDate();
    for (WaitingTimerJobEntity jobEntity : timerJobs) {
      ExecutableTimerJobEntity executableJobEntity = (ExecutableTimerJobEntity) commandContext.jobFactory().getExecutableJob(jobEntity);
      commandContext.getExecutableJobEntityManager().schedule(executableJobEntity);
      commandContext.getWaitingTimerJobEntityManager().delete(jobEntity);
    }
    return timerJobs.size();
  }

}
