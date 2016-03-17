package org.activiti.engine.test.api.runtime;

import java.util.List;
import java.util.concurrent.Callable;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.LockedJobEntity;
import org.activiti.engine.impl.test.JobTestHelper;
import org.activiti.engine.impl.test.PluggableActivitiTestCase;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.JobQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceQuery;

public class ProcessInstanceQueryAndWithExceptionTest extends PluggableActivitiTestCase {

  private static final String PROCESS_DEFINITION_KEY_NO_EXCEPTION = "oneTaskProcess";
  private static final String PROCESS_DEFINITION_KEY_WITH_EXCEPTION_1 = "JobErrorCheck";
  private static final String PROCESS_DEFINITION_KEY_WITH_EXCEPTION_2 = "JobErrorDoubleCheck";

  private org.activiti.engine.repository.Deployment deployment;

  protected void setUp() throws Exception {
    super.setUp();
    deployment = repositoryService.createDeployment()
          .addClasspathResource("org/activiti/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
          .addClasspathResource("org/activiti/engine/test/api/runtime/JobErrorCheck.bpmn20.xml")
          .addClasspathResource("org/activiti/engine/test/api/runtime/JobErrorDoubleCheck.bpmn20.xml")
          .deploy();
  }

  protected void tearDown() throws Exception {
    repositoryService.deleteDeployment(deployment.getId(), true);
    super.tearDown();
  }

  public void testQueryWithException() throws InterruptedException{
    ProcessInstance processNoException = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY_NO_EXCEPTION);
    
    ProcessInstanceQuery queryNoException = runtimeService.createProcessInstanceQuery();
    assertEquals(1, queryNoException.count());
    assertEquals(1, queryNoException.list().size());
    assertEquals(processNoException.getId(), queryNoException.list().get(0).getId());

    ProcessInstanceQuery queryWithException = runtimeService.createProcessInstanceQuery();
    assertEquals(0, queryWithException.withJobException().count());
    assertEquals(0, queryWithException.withJobException().list().size());
    
    ProcessInstance processWithException1 = startProcessInstanceWithFailingJob(PROCESS_DEFINITION_KEY_WITH_EXCEPTION_1);
    JobQuery jobQuery1 = managementService.createJobQuery().locked().processInstanceId(processWithException1.getId());

    final LockedJobEntity job = (LockedJobEntity) jobQuery1.singleResult();

    try {
      processEngineConfiguration.getCommandExecutor().execute(new Command<Object>() {

        @Override
        public Object execute(CommandContext commandContext) {
          processEngineConfiguration.getExecutableJobEntityManager().execute(job);
          return null;
        }
      });
    } catch (Throwable ex){

    }

    assertEquals(1, jobQuery1.failed().count());
    assertEquals(1, jobQuery1.failed().list().size());

    // The execution is waiting in the first usertask. This contains a
    // boundary timer event which we will execute manual for testing purposes.
    JobTestHelper.waitForJobExecutorOnCondition(processEngineConfiguration, 5000L, 100L, new Callable<Boolean>() {
      public Boolean call() throws Exception {
        return managementService.createJobQuery().withException().count() == 1;
      }
    });

    assertEquals(1, queryWithException.withJobException().count());
    assertEquals(1, queryWithException.withJobException().list().size());
    assertEquals(processWithException1.getId(), queryWithException.withJobException().list().get(0).getId());

    ProcessInstance processWithException2 = startProcessInstanceWithFailingJob(PROCESS_DEFINITION_KEY_WITH_EXCEPTION_2);
    JobQuery jobQuery2 = managementService.createJobQuery().locked().processInstanceId(processWithException2.getId());
    assertEquals(2, jobQuery2.failed().count());
    assertEquals(2, jobQuery2.failed().list().size());

    // The execution is waiting in the first usertask. This contains a
    // boundary timer event which we will execute manual for testing purposes.
    JobTestHelper.waitForJobExecutorOnCondition(processEngineConfiguration, 5000L, 100L, new Callable<Boolean>() {
      public Boolean call() throws Exception {
        return managementService.createJobQuery().withException().count() == 2;
      }
    });

    assertEquals(2, queryWithException.withJobException().count());
    assertEquals(2, queryWithException.withJobException().list().size());
    assertEquals(processWithException1.getId(), queryWithException.withJobException().processDefinitionKey(PROCESS_DEFINITION_KEY_WITH_EXCEPTION_1).list().get(0).getId());
    assertEquals(processWithException2.getId(), queryWithException.withJobException().processDefinitionKey(PROCESS_DEFINITION_KEY_WITH_EXCEPTION_2).list().get(0).getId());
  }

  private ProcessInstance startProcessInstanceWithFailingJob(String processInstanceByKey) {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processInstanceByKey);
    
    List<Job> jobList = managementService.createJobQuery()
      .processInstanceId(processInstance.getId())
      .list();

    for(Job job : jobList){
        try {
          managementService.executeJob(job.getId());
          fail("RuntimeException");
        } catch(RuntimeException re) {
      }
    }
    return processInstance;
  }

  // Test delegate
  public static class TestJavaDelegate implements JavaDelegate {
    public void execute(DelegateExecution execution){
      throw new RuntimeException();
    }
  }
}
