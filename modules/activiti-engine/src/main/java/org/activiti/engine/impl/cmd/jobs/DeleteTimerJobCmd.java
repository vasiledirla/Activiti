package org.activiti.engine.impl.cmd.jobs;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * @author Vasile Dirla
 */

public class DeleteTimerJobCmd extends DeleteJobCmd implements Serializable {

  private static final Logger log = LoggerFactory.getLogger(DeleteTimerJobCmd.class);

  public DeleteTimerJobCmd(String jobId) {
    super(jobId);
  }
  public DeleteTimerJobCmd(Job job) {
    super(job);
  }

  @Override
  protected JobEntity getJob(CommandContext commandContext, String jobId) {
    if (jobId == null) {
      throw new ActivitiIllegalArgumentException("jobId is null");
    }
    if (log.isDebugEnabled()) {
      log.debug("Deleting job {}", jobId);
    }

    JobEntity job = commandContext.getTimerJobEntityManager().findById(jobId);
    if (job == null) {
      throw new ActivitiObjectNotFoundException("No job found with id '" + jobId + "'", Job.class);
    }

    return job;
  }
}
