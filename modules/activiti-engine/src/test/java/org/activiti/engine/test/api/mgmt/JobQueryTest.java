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

package org.activiti.engine.test.api.mgmt;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.impl.cmd.CancelJobsCmd;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.persistence.entity.ExecutableJobEntity;
import org.activiti.engine.impl.persistence.entity.ExecutableJobEntityManager;
import org.activiti.engine.impl.persistence.entity.ExecutableMessageJobEntity;
import org.activiti.engine.impl.persistence.entity.ExecutableTimerJobEntity;
import org.activiti.engine.impl.persistence.entity.FailedJobEntity;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.LockedJobEntity;
import org.activiti.engine.impl.test.PluggableActivitiTestCase;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.JobQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.test.Deployment;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author Joram Barrez
 * @author Falko Menge
 */
public class JobQueryTest extends PluggableActivitiTestCase {

  private String deploymentId;
  private String messageId;
  private CommandExecutor commandExecutor;
  private ExecutableTimerJobEntity timerEntity;

  private Date testStartTime;
  private Date timerOneFireTime;
  private Date timerTwoFireTime;
  private Date timerThreeFireTime;

  private String processInstanceIdOne;
  private String processInstanceIdTwo;
  private String processInstanceIdThree;

  private static final long ONE_HOUR = 60L * 60L * 1000L;
  private static final long ONE_SECOND = 1000L;
  private static final String EXCEPTION_MESSAGE = "problem evaluating script: javax.script.ScriptException: java.lang.RuntimeException: This is an exception thrown from scriptTask";

  /**
   * Setup will create - 3 process instances, each with one timer, each firing at t1/t2/t3 + 1 hour (see process) - 1 message
   */
  protected void setUp() throws Exception {
    super.setUp();

    this.commandExecutor = processEngineConfiguration.getCommandExecutor();

    deploymentId = repositoryService.createDeployment().addClasspathResource("org/activiti/engine/test/api/mgmt/timerOnTask.bpmn20.xml").deploy().getId();

    // Create proc inst that has timer that will fire on t1 + 1 hour
    Calendar startTime = Calendar.getInstance();
    startTime.set(Calendar.MILLISECOND, 0);

    Date t1 = startTime.getTime();
    processEngineConfiguration.getClock().setCurrentTime(t1);

    processInstanceIdOne = runtimeService.startProcessInstanceByKey("timerOnTask").getId();
    testStartTime = t1;
    timerOneFireTime = new Date(t1.getTime() + ONE_HOUR);

    // Create proc inst that has timer that will fire on t2 + 1 hour
    startTime.add(Calendar.HOUR_OF_DAY, 1);
    Date t2 = startTime.getTime(); // t2 = t1 + 1 hour
    processEngineConfiguration.getClock().setCurrentTime(t2);
    processInstanceIdTwo = runtimeService.startProcessInstanceByKey("timerOnTask").getId();
    timerTwoFireTime = new Date(t2.getTime() + ONE_HOUR);

    // Create proc inst that has timer that will fire on t3 + 1 hour
    startTime.add(Calendar.HOUR_OF_DAY, 1);
    Date t3 = startTime.getTime(); // t3 = t2 + 1 hour
    processEngineConfiguration.getClock().setCurrentTime(t3);
    processInstanceIdThree = runtimeService.startProcessInstanceByKey("timerOnTask").getId();
    timerThreeFireTime = new Date(t3.getTime() + ONE_HOUR);

    // Create one message
    messageId = commandExecutor.execute(new Command<String>() {

      public String execute(CommandContext commandContext) {
        ExecutableMessageJobEntity message = commandContext.getExecutableJobEntityManager().createMessage();
        commandContext.getExecutableJobEntityManager().send(message);
        return message.getId();
      }
    });
  }

  @Override
  protected void tearDown() throws Exception {
    repositoryService.deleteDeployment(deploymentId, true);
    commandExecutor.execute(new CancelJobsCmd(messageId));
    super.tearDown();
  }

  public void testQueryByNoCriteria() {
    JobQuery query = managementService.createJobQuery();
    verifyQueryResults(query, 3);
  }

  public void testQueryByProcessInstanceId() {
    JobQuery query = managementService.createJobQuery().processInstanceId(processInstanceIdOne);
    verifyQueryResults(query, 1);
  }

  public void testQueryByInvalidProcessInstanceId() {
    JobQuery query = managementService.createJobQuery().processInstanceId("invalid");
    verifyQueryResults(query, 0);

    try {
      managementService.createJobQuery().processInstanceId(null);
      fail();
    } catch (ActivitiIllegalArgumentException e) {
    }
  }

  public void testQueryByExecutionId() {
    Job job = managementService.createJobQuery().processInstanceId(processInstanceIdOne).singleResult();
    JobQuery query = managementService.createJobQuery().executionId(job.getExecutionId());
    assertEquals(query.singleResult().getId(), job.getId());
    verifyQueryResults(query, 1);
  }

  public void testQueryByInvalidExecutionId() {
    JobQuery query = managementService.createJobQuery().executionId("invalid");
    verifyQueryResults(query, 0);

    try {
      managementService.createJobQuery().executionId(null).list();
      fail();
    } catch (ActivitiIllegalArgumentException e) {
    }
  }

  public void testQueryByRetriesLeft() {
    JobQuery query = managementService.createJobQuery().withRetriesLeft();
    verifyQueryResults(query, 3);

    setRetries(processInstanceIdOne, 0);
    // Re-running the query should give only 3 jobs now, since one job has
    // retries=0
    verifyQueryResults(query, 2);
  }

  public void testQueryByExecutable() {
    processEngineConfiguration.getClock()
            .setCurrentTime(new Date(timerThreeFireTime.getTime() + ONE_SECOND)); // all obs should be executable at t3 + 1hour.1second
    JobQuery query = managementService.createJobQuery().executable();
    verifyQueryResults(query, 3);

    // Setting retries of one job to 0, makes it non-executable
    setRetries(processInstanceIdOne, 0);
    verifyQueryResults(query, 2);

    query = managementService.createJobQuery().locked().executable();
    verifyQueryResults(query, 1);

    // Setting the clock before the start of the process instance, makes
    // none of the jobs executable
    processEngineConfiguration.getClock().setCurrentTime(testStartTime);
    verifyQueryResults(query, 0);
  }

  public void testQueryByOnlyTimers() {
    JobQuery query = managementService.createJobQuery().timers();
    verifyQueryResults(query, 3);
  }

  public void testQueryByOnlyMessages() {
    JobQuery query = managementService.createJobQuery().locked().messages();
    verifyQueryResults(query, 1);
  }

  public void testInvalidOnlyTimersUsage() {
    try {
      managementService.createJobQuery().timers().messages().list();
      fail();
    } catch (ActivitiIllegalArgumentException e) {
      assertTextPresent("Cannot combine onlyTimers() with onlyMessages() in the same query", e.getMessage());
    }
  }

  public void testQueryByDuedateLowerThan() {
    JobQuery query = managementService.createJobQuery().duedateLowerThan(testStartTime);
    verifyQueryResults(query, 0);

    query = managementService.createJobQuery().duedateLowerThan(new Date(timerOneFireTime.getTime() + ONE_SECOND));
    verifyQueryResults(query, 1);

    query = managementService.createJobQuery().duedateLowerThan(new Date(timerTwoFireTime.getTime() + ONE_SECOND));
    verifyQueryResults(query, 2);

    query = managementService.createJobQuery().duedateLowerThan(new Date(timerThreeFireTime.getTime() + ONE_SECOND));
    verifyQueryResults(query, 3);

    query = managementService.createJobQuery().locked().duedateLowerThan(new Date(timerThreeFireTime.getTime() + ONE_SECOND));
    verifyQueryResults(query, 1);

  }

  public void testQueryByDuedateHigherThan() {
    JobQuery query = managementService.createJobQuery().duedateHigherThan(testStartTime);
    verifyQueryResults(query, 3);

    query = managementService.createJobQuery().failed().duedateHigherThan(testStartTime);
    verifyQueryResults(query, 0);

    query = managementService.createJobQuery().locked().duedateHigherThan(testStartTime);
    verifyQueryResults(query, 1);

    query = managementService.createJobQuery().duedateHigherThan(timerOneFireTime);
    verifyQueryResults(query, 2);

    query = managementService.createJobQuery().locked().duedateHigherThan(timerOneFireTime);
    verifyQueryResults(query, 1);

    query = managementService.createJobQuery().duedateHigherThan(timerTwoFireTime);
    verifyQueryResults(query, 1);

    query = managementService.createJobQuery().locked().duedateHigherThan(timerTwoFireTime);
    verifyQueryResults(query, 1);

    query = managementService.createJobQuery().duedateHigherThan(timerThreeFireTime);
    verifyQueryResults(query, 0);
  }

  @Deployment(resources = { "org/activiti/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml" })
  public void testQueryByException() {
    JobQuery query = managementService.createJobQuery().failed();
    verifyQueryResults(query, 0);

    ProcessInstance processInstance = startProcessInstanceWithFailingJob();

    query = managementService.createJobQuery().processInstanceId(processInstance.getId()).failed();
    verifyFailedJob(query, processInstance);
  }

  @Deployment(resources = { "org/activiti/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml" })
  public void testQueryByExceptionMessage() {
    JobQuery query = managementService.createJobQuery().failed().exceptionMessage(EXCEPTION_MESSAGE);
    verifyQueryResults(query, 0);

    ProcessInstance processInstance = startProcessInstanceWithFailingJob();

    query = managementService.createJobQuery().failed().exceptionMessage(EXCEPTION_MESSAGE);
    verifyFailedJob(query, processInstance);
  }

  @Deployment(resources = { "org/activiti/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml" })
  public void testQueryByExceptionMessageEmpty() {
    JobQuery query = managementService.createJobQuery().exceptionMessage("");
    verifyQueryResults(query, 0);

    startProcessInstanceWithFailingJob();

    query = managementService.createJobQuery().exceptionMessage("");
    verifyQueryResults(query, 0);
  }

  public void testQueryByExceptionMessageNull() {
    try {
      managementService.createJobQuery().exceptionMessage(null);
      fail("ActivitiException expected");
    } catch (ActivitiIllegalArgumentException e) {
      assertEquals("Provided exception message is null", e.getMessage());
    }
  }

  public void testJobQueryWithExceptions() throws Throwable {

    createJobWithoutExceptionMsg();

    List<Job> list = managementService.createJobQuery().failed().list();
    assertEquals(1, list.size());

    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();

    final FailedJobEntity job1 = (FailedJobEntity) list.get(0);
    commandExecutor.execute(new Command<Void>() {

      public Void execute(CommandContext commandContext) {
        commandContext.getFailedJobEntityManager().delete(job1);
        return null;
      }
    });

    createJobWithoutExceptionStacktrace();

    list = managementService.createJobQuery().failed().list();
    assertEquals(1, list.size());
    final FailedJobEntity job2 = (FailedJobEntity) list.get(0);
    commandExecutor.execute(new Command<Void>() {

      public Void execute(CommandContext commandContext) {
        commandContext.getFailedJobEntityManager().delete(job2);
        return null;
      }
    });

  }

  // sorting //////////////////////////////////////////

  public void testQuerySorting() {
    // asc
    assertEquals(3, managementService.createJobQuery().orderByJobId().asc().count());
    assertEquals(3, managementService.createJobQuery().orderByJobDuedate().asc().count());
    assertEquals(3, managementService.createJobQuery().orderByExecutionId().asc().count());
    assertEquals(3, managementService.createJobQuery().orderByProcessInstanceId().asc().count());
    assertEquals(3, managementService.createJobQuery().orderByJobRetries().asc().count());

    // desc
    assertEquals(3, managementService.createJobQuery().orderByJobId().desc().count());
    assertEquals(3, managementService.createJobQuery().orderByJobDuedate().desc().count());
    assertEquals(3, managementService.createJobQuery().orderByExecutionId().desc().count());
    assertEquals(3, managementService.createJobQuery().orderByProcessInstanceId().desc().count());
    assertEquals(3, managementService.createJobQuery().orderByJobRetries().desc().count());

    // sorting on multiple fields
    setRetries(processInstanceIdTwo, 2);
    processEngineConfiguration.getClock().setCurrentTime(new Date(timerThreeFireTime.getTime() + ONE_SECOND)); // make
    // sure
    // all
    // timers
    // can
    // fire

    JobQuery query = managementService.createJobQuery().timers().executable().orderByJobRetries().asc().orderByJobDuedate().desc();

    List<Job> jobs = query.list();
    assertEquals(3, jobs.size());

    assertEquals(2, jobs.get(0).getRetries());
    assertEquals(3, jobs.get(1).getRetries());
    assertEquals(3, jobs.get(2).getRetries());

    assertEquals(processInstanceIdTwo, jobs.get(0).getProcessInstanceId());
    assertEquals(processInstanceIdThree, jobs.get(1).getProcessInstanceId());
    assertEquals(processInstanceIdOne, jobs.get(2).getProcessInstanceId());
  }

  public void testQueryInvalidSortingUsage() {
    try {
      managementService.createJobQuery().orderByJobId().list();
      fail();
    } catch (ActivitiIllegalArgumentException e) {
      assertTextPresent("call asc() or desc() after using orderByXX()", e.getMessage());
    }

    try {
      managementService.createJobQuery().asc();
      fail();
    } catch (ActivitiIllegalArgumentException e) {
      assertTextPresent("You should call any of the orderBy methods first before specifying a direction", e.getMessage());
    }
  }

  // helper ////////////////////////////////////////////////////////////

  private void setRetries(final String processInstanceId, final int retries) {
    final Job job = managementService.createJobQuery().processInstanceId(processInstanceId).singleResult();
    commandExecutor.execute(new Command<Void>() {

      public Void execute(CommandContext commandContext) {
        JobEntity timer = commandContext.getDbSqlSession().selectById(ExecutableJobEntity.class, job.getId());
        timer.setRetries(retries);
        return null;
      }

    });
  }

  private ProcessInstance startProcessInstanceWithFailingJob() {
    // start a process with a failing job
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

    // The execution is waiting in the first usertask. This contains a boundary
    // timer event which we will execute manual for testing purposes.
    Job timerJob = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();

    assertNotNull("No job found for process instance", timerJob);

    try {
      managementService.executeJob(timerJob.getId());
      fail("RuntimeException from within the script task expected");
    } catch (RuntimeException re) {
      assertTextPresent(EXCEPTION_MESSAGE, re.getCause().getMessage());
    }
    return processInstance;
  }

  private void verifyFailedJob(JobQuery query, ProcessInstance processInstance) {
    verifyQueryResults(query, 1);

    Job failedJob = query.singleResult();
    assertNotNull(failedJob);
    assertEquals(processInstance.getId(), failedJob.getProcessInstanceId());
    assertNotNull(failedJob.getExceptionMessage());
    assertTextPresent(EXCEPTION_MESSAGE, failedJob.getExceptionMessage());
  }

  private void verifyQueryResults(JobQuery query, int countExpected) {
    assertEquals(countExpected, query.list().size());
    assertEquals(countExpected, query.count());

    if (countExpected == 1) {
      assertNotNull(query.singleResult());
    } else if (countExpected > 1) {
      verifySingleResultFails(query);
    } else if (countExpected == 0) {
      assertNull(query.singleResult());
    }
  }

  private void verifySingleResultFails(JobQuery query) {
    try {
      query.singleResult();
      fail();
    } catch (ActivitiException e) {
    }
  }

  private void createJobWithoutExceptionMsg() {
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
    commandExecutor.execute(new Command<Void>() {

      public Void execute(CommandContext commandContext) {
        ExecutableJobEntityManager jobManager = commandContext.getExecutableJobEntityManager();

        timerEntity = jobManager.createTimer();
        timerEntity.setDuedate(new Date());
        timerEntity.setRetries(0);

        StringWriter stringWriter = new StringWriter();
        NullPointerException exception = new NullPointerException();
        exception.printStackTrace(new PrintWriter(stringWriter));
        timerEntity.setExceptionStacktrace(stringWriter.toString());

        commandContext.getExecutableJobEntityManager().insert(timerEntity);

        LockedJobEntity lockedJobEntity = commandContext.getExecutableJobEntityManager().lockJob(timerEntity, UUID.randomUUID().toString(), new Date());

        FailedJobEntity failedJobEntity = commandContext.jobFactory().getFailedJob(lockedJobEntity);

        commandContext.getLockedJobEntityManager().delete(lockedJobEntity);
        commandContext.getFailedJobEntityManager().insert(failedJobEntity);

        assertNotNull(failedJobEntity.getId());
        return null;

      }
    });

  }

  private void createJobWithoutExceptionStacktrace() {
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
    commandExecutor.execute(new Command<Void>() {

      public Void execute(CommandContext commandContext) {
        ExecutableJobEntityManager jobManager = commandContext.getExecutableJobEntityManager();

        timerEntity = jobManager.createTimer();
        timerEntity.setDuedate(new Date());
        timerEntity.setRetries(0);
        timerEntity.setExceptionMessage("I'm supposed to fail");

        LockedJobEntity lockedJobEntity = commandContext.jobFactory().getLockedJob(timerEntity, new Date(), UUID.randomUUID().toString());
        FailedJobEntity failedJobEntity = commandContext.jobFactory().getFailedJob(lockedJobEntity);

        commandContext.getFailedJobEntityManager().insert(failedJobEntity);

        assertNotNull(failedJobEntity.getId());

        return null;

      }
    });

  }

}
