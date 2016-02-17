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

package org.activiti.engine.impl.cmd;

import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.runtime.Job;

import java.io.Serializable;

/**
 * @author Frederik Heremans
 * @author Vasile Dirla
 */
public class GetJobExceptionStacktraceCmd extends JobCmd<String> implements Serializable {

  private static final long serialVersionUID = 1L;
  private String jobId;

  public GetJobExceptionStacktraceCmd(String jobType, String jobId) {
    super(jobType);
    this.jobId = jobId;
  }

  public GetJobExceptionStacktraceCmd(String jobId) {
    super(Job.GENERIC);
    this.jobId = jobId;
  }

  public String executeCommand(CommandContext commandContext) {
    if (jobId == null) {
      throw new ActivitiIllegalArgumentException("jobId is null");
    }

    JobEntity job = getJobEntityManager().findById(jobId);

    if (job == null) {
      throw new ActivitiObjectNotFoundException("No job found with id " + jobId, Job.class);
    }

    return job.getExceptionStacktrace();
  }

}
