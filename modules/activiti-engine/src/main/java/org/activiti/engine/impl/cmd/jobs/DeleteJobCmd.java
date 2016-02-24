package org.activiti.engine.impl.cmd.jobs;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.compatibility.Activiti5CompatibilityHandler;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.JobEntityManager;
import org.activiti.engine.impl.util.Activiti5Util;
import org.activiti.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * @author Saeid Mirzaei
 * @author Joram Barrez
 */

public class DeleteJobCmd extends JobCmd<Object> implements Serializable {

  private static final Logger log = LoggerFactory.getLogger(DeleteJobCmd.class);
  private static final long serialVersionUID = 1L;

  protected String jobId;

  public DeleteJobCmd(String jobId) {
    this.jobId = jobId;
  }

  public DeleteJobCmd(Job job) {
    this(job.getId());
  }

  public Object executeCommand(CommandContext commandContext) {
    JobEntity jobToDelete = getJob(commandContext, jobId);

    // We need to check if the job was locked, ie acquired by the job acquisition thread
    // This happens if the the job was already acquired, but not yet executed.
    // In that case, we can't allow to delete the job.
    if (jobToDelete.getLockOwner() != null) {
      throw new ActivitiException("Cannot delete job when the job is being executed. Try again later.");
    }

    if (Activiti5Util.isActiviti5ProcessDefinitionId(commandContext, jobToDelete.getProcessDefinitionId())) {
      Activiti5CompatibilityHandler activiti5CompatibilityHandler = Activiti5Util.getActiviti5CompatibilityHandler();
      activiti5CompatibilityHandler.deleteJob(jobToDelete.getId());
      return null;
    }

    sendCancelEvent(jobToDelete);

    commandContext.getJobEntityManager(jobToDelete.getJobType()).delete(jobToDelete);
    return null;
  }

  protected void sendCancelEvent(JobEntity jobToDelete) {
    if (Context.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
      Context.getProcessEngineConfiguration().getEventDispatcher()
              .dispatchEvent(ActivitiEventBuilder.createEntityEvent(ActivitiEventType.JOB_CANCELED, jobToDelete));
    }
  }

  @Override
  public JobEntityManager getJobEntityManager(CommandContext commandContext) {
    return null;
  }

}
