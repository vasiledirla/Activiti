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
package org.activiti.engine.test.jobexecutor;

import java.util.Date;

import org.activiti.engine.impl.asyncexecutor.AcquiredExecutableJobEntities;
import org.activiti.engine.impl.cmd.AcquireExecutableJobsDueCmd;
import org.activiti.engine.impl.cmd.ExecuteAsyncJobCmd;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.persistence.entity.ExecutableMessageJobEntity;
import org.activiti.engine.impl.persistence.entity.ExecutableTimerJobEntity;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.LockedJobEntity;
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.activiti.engine.runtime.Job;

/**
 * @author Tom Baeyens
 */
public class JobExecutorCmdHappyTest extends JobExecutorTestCase {

  public void testJobCommandsWithMessage() {
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();

    String jobId = commandExecutor.execute(new Command<String>() {

      public String execute(CommandContext commandContext) {
        ExecutableMessageJobEntity message = createTweetMessage("i'm coding a test");
        commandContext.getExecutableJobEntityManager().send(message);
        return message.getId();
      }
    });

    Job job = managementService.createJobQuery().locked().singleResult();
    assertNotNull(job);
    assertEquals(jobId, job.getId());

    assertEquals(0, tweetHandler.getMessages().size());

    managementService.executeJob(job.getId());

    assertEquals("i'm coding a test", tweetHandler.getMessages().get(0));
    assertEquals(1, tweetHandler.getMessages().size());
  }

  static final long SOME_TIME = 928374923546L;
  static final long SECOND = 1000;

  public void testJobCommandsWithTimer() {
    // clock gets automatically reset in LogTestCase.runTest
    processEngineConfiguration.getClock().setCurrentTime(new Date(SOME_TIME));

    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();

    String jobId = commandExecutor.execute(new Command<String>() {

      public String execute(CommandContext commandContext) {
        ExecutableTimerJobEntity timer = createTweetTimer("i'm coding a test", new Date(SOME_TIME + (10 * SECOND)));
        commandContext.getExecutableJobEntityManager().schedule(timer);
        return timer.getId();
      }
    });

    AcquiredExecutableJobEntities acquiredJobs = commandExecutor.execute(new AcquireExecutableJobsDueCmd(processEngineConfiguration.getAsyncExecutor()));
    assertEquals(0, acquiredJobs.size());

    processEngineConfiguration.getClock().setCurrentTime(new Date(SOME_TIME + (20 * SECOND)));

    acquiredJobs = commandExecutor.execute(new AcquireExecutableJobsDueCmd(processEngineConfiguration.getAsyncExecutor()));
    assertEquals(1, acquiredJobs.size());

    LockedJobEntity job = acquiredJobs.getJobs().iterator().next();

    assertEquals(jobId, job.getId());

    assertEquals(0, tweetHandler.getMessages().size());

    commandExecutor.execute(new ExecuteAsyncJobCmd(job));

    assertEquals("i'm coding a test", tweetHandler.getMessages().get(0));
    assertEquals(1, tweetHandler.getMessages().size());
  }
}