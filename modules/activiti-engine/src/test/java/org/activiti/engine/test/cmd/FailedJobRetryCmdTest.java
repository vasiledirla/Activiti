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
package org.activiti.engine.test.cmd;

import org.activiti.engine.impl.cmd.CleanupFailedJobsCommand;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.test.PluggableActivitiTestCase;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.test.Deployment;

/**
 * @author Saeid Mirzaei
 */
public class FailedJobRetryCmdTest extends PluggableActivitiTestCase {

  private void waitForExecutedJobWithRetriesLeft(final int retriesLeft) {

    Job job = managementService.createJobQuery().singleResult();
    if (job == null) {
      job = managementService.createJobQuery().locked().singleResult();
    }

    try {
      managementService.executeJob(job.getId());
    } catch (Exception e) {
    }

    // update job
    job = managementService.createJobQuery().failed().singleResult();
    if (job != null) {
      if (job.getRetries() > retriesLeft) {
        waitForExecutedJobWithRetriesLeft(retriesLeft);
      }
    }
  }

  private void stillOneJobWithExceptionAndRetriesLeft() {
    assertEquals(1, managementService.createJobQuery().failed().count());
    assertEquals(1, managementService.createJobQuery().failed().withRetriesLeft().count());
  }

  private Job fetchJob(String processInstanceId) {
    processEngineConfiguration.getCommandExecutor().execute(new CleanupFailedJobsCommand());
    return managementService.createJobQuery().processInstanceId(processInstanceId).singleResult();
  }

  private ExecutionEntity fetchExecutionEntity(String processInstanceId) {
    return (ExecutionEntity) runtimeService.createExecutionQuery().processInstanceId(processInstanceId).onlyProcessInstanceExecutions().singleResult();
  }

  private Job refreshJob(String jobId) {
    processEngineConfiguration.getCommandExecutor().execute(new CleanupFailedJobsCommand());
    return managementService.createJobQuery().jobId(jobId).singleResult();
  }

  private ExecutionEntity refreshExecutionEntity(String executionId) {
    return (ExecutionEntity) runtimeService.createExecutionQuery().executionId(executionId).singleResult();
  }

  @Deployment(resources = { "org/activiti/engine/test/cmd/FailedJobRetryCmdTest.testFailedServiceTask.bpmn20.xml" })
  public void testFailedServiceTask() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("failedServiceTask");
    assertNotNull(pi);
    waitForExecutedJobWithRetriesLeft(4);

    stillOneJobWithExceptionAndRetriesLeft();

    Job job = fetchJob(pi.getProcessInstanceId());
    assertNotNull(job);
    assertEquals(pi.getProcessInstanceId(), job.getProcessInstanceId());

    assertEquals(4, job.getRetries());

    Execution execution = runtimeService.createExecutionQuery().onlyChildExecutions().processInstanceId(pi.getId()).singleResult();
    assertEquals("failingServiceTask", execution.getActivityId());

    waitForExecutedJobWithRetriesLeft(3);


    stillOneJobWithExceptionAndRetriesLeft();

    job = refreshJob(job.getId());
    assertEquals(3, job.getRetries());

    execution = refreshExecutionEntity(execution.getId());
    assertEquals("failingServiceTask", execution.getActivityId());

    waitForExecutedJobWithRetriesLeft(2);


    stillOneJobWithExceptionAndRetriesLeft();
    job = refreshJob(job.getId());
    assertEquals(2, job.getRetries());

    execution = refreshExecutionEntity(execution.getId());
    assertEquals("failingServiceTask", execution.getActivityId());

    waitForExecutedJobWithRetriesLeft(1);

    stillOneJobWithExceptionAndRetriesLeft();
    job = refreshJob(job.getId());
    assertEquals(1, job.getRetries());

    execution = refreshExecutionEntity(execution.getId());
    assertEquals("failingServiceTask", execution.getActivityId());

    waitForExecutedJobWithRetriesLeft(0);

    job = managementService.createJobQuery().failed().jobId(job.getId()).singleResult();

    assertEquals(0, job.getRetries());
    assertEquals(1, managementService.createJobQuery().failed().count());
    assertEquals(0, managementService.createJobQuery().failed().withRetriesLeft().count());
    assertEquals(1, managementService.createJobQuery().failed().noRetriesLeft().count());

    execution = refreshExecutionEntity(execution.getId());
    assertEquals("failingServiceTask", execution.getActivityId());

  }

}
