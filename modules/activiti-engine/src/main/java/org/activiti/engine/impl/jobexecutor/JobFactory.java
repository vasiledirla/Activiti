package org.activiti.engine.impl.jobexecutor;

import org.activiti.engine.impl.persistence.entity.ExecutableJobEntity;
import org.activiti.engine.impl.persistence.entity.FailedJobEntity;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.LockedJobEntity;
import org.activiti.engine.runtime.Job;

import java.util.Date;

public interface JobFactory {

  LockedJobEntity getLockedJob(ExecutableJobEntity executableJobEntity, Date time, String lockOwner);

  FailedJobEntity getFailedJob(LockedJobEntity lockedJobEntity);

  ExecutableJobEntity getExecutableJob(JobEntity failedJobEntity);
}
