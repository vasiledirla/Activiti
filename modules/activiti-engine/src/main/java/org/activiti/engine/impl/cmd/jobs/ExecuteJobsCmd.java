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
package org.activiti.engine.impl.cmd.jobs;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.JobNotFoundException;
import org.activiti.engine.compatibility.Activiti5CompatibilityHandler;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.impl.cfg.TransactionState;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandContextCloseListener;
import org.activiti.engine.impl.jobexecutor.FailedJobListener;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.JobEntityManager;
import org.activiti.engine.impl.util.Activiti5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Vasile Dirla
 */
public class ExecuteJobsCmd extends JobCmd<Object> implements Serializable {

  private static final long serialVersionUID = 1L;

  private static Logger log = LoggerFactory.getLogger(ExecuteJobsCmd.class);

  protected String jobId;
  protected JobEntity job;

  public ExecuteJobsCmd(String jobId) {
    this.jobId = jobId;
  }

  public ExecuteJobsCmd(JobEntity job) {
    this.job = job;
  }

  public Object executeCommand(CommandContext commandContext) {

    if (jobId == null && job == null) {
      throw new ActivitiIllegalArgumentException("jobId and job is null");
    }

    if (job == null) {
      job = getJob(commandContext, jobId);
    }

    if (job == null) {
      throw new JobNotFoundException(jobId);
    }

    if (log.isDebugEnabled()) {
      log.debug("Executing job {}", job.getId());
    }

    if (job.getProcessDefinitionId() != null && Activiti5Util.isActiviti5ProcessDefinitionId(commandContext, job.getProcessDefinitionId())) {
      Activiti5CompatibilityHandler activiti5CompatibilityHandler = Activiti5Util.getActiviti5CompatibilityHandler();
      activiti5CompatibilityHandler.executeJob(job);
      return null;
    }

    commandContext.addCloseListener(new ManualJobExecutionCommandContextCloseListener(job));

    try {
      commandContext.getJobEntityManager(job.getJobType()).execute(job);
    } catch (Throwable exception) {
      // Finally, Throw the exception to indicate the ExecuteJobCmd failed
      throw new ActivitiException("Job " + jobId + " failed", exception);
    }

    return null;
  }
  @Override
  public JobEntityManager getJobEntityManager(CommandContext commandContext) {
    return null;
  }

  public String getJobId() {
    return jobId;
  }

  public static final class ManualJobExecutionCommandContextCloseListener implements CommandContextCloseListener {

    protected JobEntity jobEntity;

    public ManualJobExecutionCommandContextCloseListener(JobEntity jobEntity) {
      this.jobEntity = jobEntity;
    }

    @Override
    public void closing(CommandContext context) {

      if (context.getException() != null) {

        FailedJobListener failedJobListener = null;

        // When transaction is rolled back, decrement retries
        failedJobListener = new FailedJobListener(context.getProcessEngineConfiguration().getCommandExecutor(), jobEntity.getId(), jobEntity.getJobType());
        failedJobListener.setException(context.getException());
        context.getTransactionContext().addTransactionListener(TransactionState.ROLLED_BACK, failedJobListener);

        if (context.getEventDispatcher().isEnabled()) {
          try {
            context.getEventDispatcher()
                    .dispatchEvent(ActivitiEventBuilder.createEntityExceptionEvent(ActivitiEventType.JOB_EXECUTION_FAILURE, jobEntity, context.getException()));
          } catch (Throwable ignore) {
            log.warn("Exception occurred while dispatching job failure event, ignoring.", ignore);
          }
        }

      } else {

        if (context.getEventDispatcher().isEnabled()) {
          context.getEventDispatcher().dispatchEvent(ActivitiEventBuilder.createEntityEvent(ActivitiEventType.JOB_EXECUTION_SUCCESS, jobEntity));
        }

      }
    }

    @Override
    public void closed(CommandContext commandContext) {

    }

  }

}
