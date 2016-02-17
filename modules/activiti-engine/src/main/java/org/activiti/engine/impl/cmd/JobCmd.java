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
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.JobEntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vasile Dirla
 */

public abstract class JobCmd<T> implements Command<T> {

  private static final Logger log = LoggerFactory.getLogger(JobCmd.class.getName());

  protected String jobType;
  private JobEntityManager jobEntityManager;

  public JobCmd(String jobType) {
    if (jobType == null) {
      throw new ActivitiIllegalArgumentException("The jobType is mandatory, but '" + jobType + "' has been provided.");
    }
    this.jobType = jobType;
  }

  public T execute(CommandContext commandContext) {
    jobEntityManager = commandContext.getJobEntityManager(jobType);
    return executeCommand(commandContext);
  }
  protected abstract T executeCommand(CommandContext commandContext);

  public JobEntityManager getJobEntityManager() {
    return jobEntityManager;
  }
  public void setJobEntityManager(JobEntityManager entityManager) {
    this.jobEntityManager = entityManager;
  }
}
