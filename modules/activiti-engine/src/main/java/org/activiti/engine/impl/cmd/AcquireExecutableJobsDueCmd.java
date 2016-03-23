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
package org.activiti.engine.impl.cmd;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.asyncexecutor.AcquiredExecutableJobEntities;
import org.activiti.engine.impl.asyncexecutor.AsyncExecutor;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutableJobEntity;
import org.activiti.engine.impl.persistence.entity.LockedJobEntity;

/**
 * @author Tijs Rademakers
 * @author Vasile Dirla
 */
public class AcquireExecutableJobsDueCmd implements Command<AcquiredExecutableJobEntities> {

  private final AsyncExecutor asyncExecutor;

  public AcquireExecutableJobsDueCmd(AsyncExecutor asyncExecutor) {
    this.asyncExecutor = asyncExecutor;
  }

  public AcquiredExecutableJobEntities execute(CommandContext commandContext) {
    AcquiredExecutableJobEntities acquiredJobs = new AcquiredExecutableJobEntities();
    List<ExecutableJobEntity> jobs = commandContext.getExecutableJobEntityManager().findExecutableJobsDueToExecute(new Page(0, asyncExecutor.getMaxAsyncJobsDuePerAcquisition()));

    GregorianCalendar gregorianCalendar = new GregorianCalendar();
    gregorianCalendar.setTime(commandContext.getProcessEngineConfiguration().getClock().getCurrentTime());
    gregorianCalendar.add(Calendar.MILLISECOND, asyncExecutor.getAsyncJobLockTimeInMillis());

    for (ExecutableJobEntity job : jobs) {
      LockedJobEntity lockedJobEntity = commandContext.getExecutableJobEntityManager().lockJob(job, null, gregorianCalendar.getTime());
      acquiredJobs.addJob(lockedJobEntity);
    }

    return acquiredJobs;
  }

}