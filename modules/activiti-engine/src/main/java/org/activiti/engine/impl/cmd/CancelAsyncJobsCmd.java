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

import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.runtime.Job;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Send job cancelled event and delete job
 *
 * @author Vasile Dirla
 */
public class CancelAsyncJobsCmd extends JobCmd<Void> implements Serializable {

  private static final long serialVersionUID = 1L;
  List<String> jobIds;

  public CancelAsyncJobsCmd(List<String> jobIds) {
    super(Job.MESSAGE);
    this.jobIds = jobIds;
  }

  public CancelAsyncJobsCmd(String jobId) {
    super(Job.MESSAGE);
    this.jobIds = new ArrayList<String>();
    jobIds.add(jobId);
  }

  public Void executeCommand(CommandContext commandContext) {
    JobEntity jobToDelete = null;
    for (String jobId : jobIds) {
      jobToDelete = getJobEntityManager().findById(jobId);

      if (jobToDelete != null) {
        // When given job doesn't exist, ignore
        if (commandContext.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
          commandContext.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(ActivitiEventBuilder.createEntityEvent(ActivitiEventType.JOB_CANCELED, jobToDelete));
        }

        getJobEntityManager().delete(jobToDelete);
      }
    }
    return null;
  }
}
