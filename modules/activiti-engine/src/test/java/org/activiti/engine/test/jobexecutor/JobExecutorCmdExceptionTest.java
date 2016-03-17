/**
 * 
 */
package org.activiti.engine.test.jobexecutor;

import org.activiti.engine.impl.asyncexecutor.CleanupJobsRunnable;
import org.activiti.engine.impl.cmd.CleanupFailedJobsCommand;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.persistence.entity.ExecutableMessageJobEntity;
import org.activiti.engine.impl.persistence.entity.ExecutableMessageJobEntityImpl;
import org.activiti.engine.impl.test.PluggableActivitiTestCase;
import org.activiti.engine.runtime.Job;

/**
 * @author Tom Baeyens
 */
public class JobExecutorCmdExceptionTest extends PluggableActivitiTestCase {

  protected TweetExceptionHandler tweetExceptionHandler = new TweetExceptionHandler();

  private CommandExecutor commandExecutor;

  public void setUp() throws Exception {
    processEngineConfiguration.getJobHandlers().put(tweetExceptionHandler.getType(), tweetExceptionHandler);
    this.commandExecutor = processEngineConfiguration.getCommandExecutor();
  }

  public void tearDown() throws Exception {
    processEngineConfiguration.getJobHandlers().remove(tweetExceptionHandler.getType());
  }

  public void testJobCommandsWith2Exceptions() {
    commandExecutor.execute(new Command<String>() {

      public String execute(CommandContext commandContext) {
        ExecutableMessageJobEntity message = createTweetExceptionMessage();
        commandContext.getExecutableJobEntityManager().send(message);
        return message.getId();
      }
    });

    Job job = managementService.createJobQuery().locked().singleResult();
    assertEquals(3, job.getRetries());

    try {
      managementService.executeJob(job.getId());
      fail("exception expected");
    } catch (Exception e) {
      // exception expected;
    }

    job = managementService.createJobQuery().failed().singleResult();
    assertEquals(2, job.getRetries());

    try {
      managementService.executeJob(job.getId());
      fail("exception expected because cannot execute a job in failed state");
    } catch (Exception e) {
      // exception expected;
    }

    // trying to execute a faield job will not affect it.
    job = managementService.createJobQuery().failed().singleResult();
    assertEquals(2, job.getRetries());

    int cleanedJobs = commandExecutor.execute(new CleanupFailedJobsCommand());

    if (cleanedJobs>0){
      job = managementService.createJobQuery().singleResult();
    }
    assertEquals(2, job.getRetries());



    try {
      managementService.executeJob(job.getId());
      fail("exception expected");
    } catch (Exception e) {
      // exception expected;
    }

    // trying to execute a faield job will not affect it.
    job = managementService.createJobQuery().failed().singleResult();
    assertEquals(1, job.getRetries());

    cleanedJobs = commandExecutor.execute(new CleanupFailedJobsCommand());

    if (cleanedJobs>0){
      job = managementService.createJobQuery().singleResult();
    }
    assertEquals(1, job.getRetries());

    managementService.executeJob(job.getId());
  }

  public void testJobCommandsWith3Exceptions() {
    tweetExceptionHandler.setExceptionsRemaining(3);

    commandExecutor.execute(new Command<String>() {

      public String execute(CommandContext commandContext) {
        ExecutableMessageJobEntity message = createTweetExceptionMessage();
        commandContext.getExecutableJobEntityManager().send(message);
        return message.getId();
      }
    });

    Job job = managementService.createJobQuery().locked().singleResult();
    assertEquals(3, job.getRetries());

    try {
      managementService.executeJob(job.getId());
      fail("exception expected");
    } catch (Exception e) {
      // exception expected;
    }


    // trying to execute a faield job will not affect it.
    job = managementService.createJobQuery().failed().singleResult();
    assertEquals(2, job.getRetries());

    int cleanedJobs = commandExecutor.execute(new CleanupFailedJobsCommand());

    if (cleanedJobs>0){
      job = managementService.createJobQuery().singleResult();
    }
    assertEquals(2, job.getRetries());


    job = managementService.createJobQuery().singleResult();
    assertEquals(2, job.getRetries());

    try {
      managementService.executeJob(job.getId());
      fail("exception expected");
    } catch (Exception e) {
      // exception expected;
    }



    // trying to execute a faield job will not affect it.
    job = managementService.createJobQuery().failed().singleResult();
    assertEquals(1, job.getRetries());

    cleanedJobs = commandExecutor.execute(new CleanupFailedJobsCommand());

    if (cleanedJobs>0){
      job = managementService.createJobQuery().singleResult();
    }
    assertEquals(1, job.getRetries());

    job = managementService.createJobQuery().singleResult();
    assertEquals(1, job.getRetries());

    try {
      managementService.executeJob(job.getId());
      fail("exception expected");
    } catch (Exception e) {
      // exception expected;
    }

    job = managementService.createJobQuery().failed().singleResult();
    assertEquals(0, job.getRetries());

    managementService.deleteJob(job.getId());
  }

  protected ExecutableMessageJobEntity createTweetExceptionMessage() {
    ExecutableMessageJobEntity message = new ExecutableMessageJobEntityImpl();
    message.setJobHandlerType("tweet-exception");
    return message;
  }
}
