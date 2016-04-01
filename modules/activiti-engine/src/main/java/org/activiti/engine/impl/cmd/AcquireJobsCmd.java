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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.jobexecutor.AcquiredJobs;
import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.activiti.engine.impl.persistence.entity.ExecutableJobEntity;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.LockedJobEntity;
import org.activiti.engine.impl.persistence.entity.MessageEntity;

/**
 * @author Nick Burch
 * @author Daniel Meyer
 */
public class AcquireJobsCmd implements Command<AcquiredJobs> {

  private final JobExecutor jobExecutor;

  public AcquireJobsCmd(JobExecutor jobExecutor) {
    this.jobExecutor = jobExecutor;
  }

  public AcquiredJobs execute(CommandContext commandContext) {

    String lockOwner = jobExecutor.getLockOwner();
    int lockTimeInMillis = jobExecutor.getLockTimeInMillis();
    int maxNonExclusiveJobsPerAcquisition = jobExecutor.getMaxJobsPerAcquisition();

    AcquiredJobs acquiredJobs = new AcquiredJobs();
    List<ExecutableJobEntity> jobs = commandContext.getExecutableJobEntityManager().findNextJobsToExecute(new Page(0, maxNonExclusiveJobsPerAcquisition));

    for (ExecutableJobEntity job : jobs) {
      List<String> jobIds = new ArrayList<String>();
      if (job != null && !acquiredJobs.contains(job.getId())) {
        if (job instanceof MessageEntity && job.isExclusive() && job.getProcessInstanceId() != null) {
          // wait to get exclusive jobs within 100ms
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
          }

          // acquire all exclusive jobs in the same process instance
          // (includes the current job)
          List<ExecutableJobEntity> exclusiveJobs = commandContext.getExecutableJobEntityManager().findExclusiveJobsToExecute(job.getProcessInstanceId());
          for (ExecutableJobEntity exclusiveJob : exclusiveJobs) {
            if (exclusiveJob != null) {
              lockJob(commandContext, exclusiveJob, lockOwner, lockTimeInMillis);
              jobIds.add(exclusiveJob.getId());
            }
          }

        } else {
          lockJob(commandContext, job, lockOwner, lockTimeInMillis);
          jobIds.add(job.getId());
        }

      }

      acquiredJobs.addJobIdBatch(jobIds);
    }

    return acquiredJobs;
  }

  protected LockedJobEntity lockJob(CommandContext commandContext, ExecutableJobEntity job, String lockOwner, int lockTimeInMillis) {

    GregorianCalendar gregorianCalendar = new GregorianCalendar();
    gregorianCalendar.setTime(commandContext.getProcessEngineConfiguration().getClock().getCurrentTime());
    gregorianCalendar.add(Calendar.MILLISECOND, lockTimeInMillis);

    return commandContext.getExecutableJobEntityManager().lockJob(job, lockOwner, gregorianCalendar.getTime());
  }
}