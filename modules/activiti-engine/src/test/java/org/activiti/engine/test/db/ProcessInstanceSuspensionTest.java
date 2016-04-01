package org.activiti.engine.test.db;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.asyncexecutor.AcquiredExecutableJobEntities;
import org.activiti.engine.impl.cmd.AcquireExecutableJobsDueCmd;
import org.activiti.engine.impl.cmd.AcquireJobsCmd;
import org.activiti.engine.impl.cmd.MoveTimerJobsDueDate;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.jobexecutor.GetUnlockedTimersByDuedateCmd;
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.activiti.engine.impl.persistence.entity.WaitingTimerJobEntity;
import org.activiti.engine.impl.test.PluggableActivitiTestCase;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.test.Deployment;

/**
 * 
 * @author Daniel Meyer
 */
public class ProcessInstanceSuspensionTest extends PluggableActivitiTestCase {

  @Deployment(resources = { "org/activiti/engine/test/db/oneJobProcess.bpmn20.xml" })
  public void testJobsNotVisibleToAcquisitionIfInstanceSuspended() {

    ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance pi = runtimeService.startProcessInstanceByKey(pd.getKey());

    // now there is one job:
    // now there is one job:
    Job job = managementService.createJobQuery().waitingTimers().singleResult();
    assertNotNull(job);

    makeSureJobDue(job);

    managementService.executeCommand(new MoveTimerJobsDueDate());

    // the acquirejobs command sees the job:
    AcquiredExecutableJobEntities acquiredJobs = executeAcquireJobsCommand();
    assertEquals(1, acquiredJobs.size());

    // suspend the process instance:
    runtimeService.suspendProcessInstanceById(pi.getId());

    managementService.executeCommand(new MoveTimerJobsDueDate());
    // now, the acquirejobs command does not see the job:
    acquiredJobs = executeAcquireJobsCommand();
    assertEquals(0, acquiredJobs.size());
  }

  @Deployment(resources = { "org/activiti/engine/test/db/oneJobProcess.bpmn20.xml" })
  public void testJobsNotVisibleToAcquisitionIfDefinitionSuspended() {

    ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey(pd.getKey());
    // now there is one job:
    Job job = managementService.createJobQuery().waitingTimers().singleResult();
    assertNotNull(job);

    makeSureJobDue(job);

    managementService.executeCommand(new MoveTimerJobsDueDate());

    // the acquirejobs command sees the job:
    AcquiredExecutableJobEntities acquiredJobs = executeAcquireJobsCommand();
    assertEquals(1, acquiredJobs.size());

    // suspend the process instance:
    repositoryService.suspendProcessDefinitionById(pd.getId());

    managementService.executeCommand(new MoveTimerJobsDueDate());
    // now, the acquirejobs command does not see the job:
    acquiredJobs = executeAcquireJobsCommand();
    assertEquals(0, acquiredJobs.size());
  }

  @Deployment
  public void testSuspendedProcessTimerExecution() throws Exception {
    // Process with boundary timer-event that fires in 1 hour
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey("suspendProcess");
    assertNotNull(procInst);
    assertEquals(1, managementService.createJobQuery().processInstanceId(procInst.getId()).waitingTimers().count());
    assertEquals(0, managementService.createJobQuery().processInstanceId(procInst.getId()).failed().count());
    assertEquals(0, managementService.createJobQuery().processInstanceId(procInst.getId()).locked().count());
    assertEquals(0, managementService.createJobQuery().processInstanceId(procInst.getId()).count());

    // Roll time ahead to be sure timer is due to fire
    Calendar tomorrow = Calendar.getInstance();
    tomorrow.add(Calendar.DAY_OF_YEAR, 1);
    processEngineConfiguration.getClock().setCurrentTime(tomorrow.getTime());

    // Check if timer is eligible to be executed, when process in not yet
    // suspended
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
    List<WaitingTimerJobEntity> jobs = commandExecutor.execute(new GetUnlockedTimersByDuedateCmd(processEngineConfiguration.getClock().getCurrentTime(), new Page(0, 1)));
    assertEquals(1, jobs.size());

    // Suspend process instance
    runtimeService.suspendProcessInstanceById(procInst.getId());

    // Check if the timer is NOT acquired, even though the duedate is reached
    jobs = commandExecutor.execute(new GetUnlockedTimersByDuedateCmd(processEngineConfiguration.getClock().getCurrentTime(), new Page(0, 1)));
    assertEquals(0, jobs.size());
  }

  protected void makeSureJobDue(final Job job) {
    processEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {
      public Void execute(CommandContext commandContext) {
        Date currentTime = processEngineConfiguration.getClock().getCurrentTime();
        commandContext.getWaitingTimerJobEntityManager().findById(job.getId()).setDuedate(new Date(currentTime.getTime() - 10000));
        return null;
      }

    });
  }

  private AcquiredExecutableJobEntities executeAcquireJobsCommand() {
    return processEngineConfiguration.getCommandExecutor().execute(new AcquireExecutableJobsDueCmd(processEngineConfiguration.getAsyncExecutor()));
  }

}
