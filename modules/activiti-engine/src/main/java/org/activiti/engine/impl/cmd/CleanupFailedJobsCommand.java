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

import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutableJobEntity;
import org.activiti.engine.impl.persistence.entity.FailedJobEntity;

import java.util.List;

public class CleanupFailedJobsCommand implements Command<Integer> {

  protected int maxJobsPerAcquisition = 10;

  public int getMaxJobsPerAcquisition() {
    return maxJobsPerAcquisition;
  }
  public void setMaxJobsPerAcquisition(int maxJobsPerAcquisition) {
    this.maxJobsPerAcquisition = maxJobsPerAcquisition;
  }
  @Override
  public Integer execute(CommandContext commandContext) {
    List<FailedJobEntity> retriableJobs = commandContext.getFailedJobEntityManager().selectRetriableJobs(new Page(0, maxJobsPerAcquisition));

    for (FailedJobEntity jobEntity : retriableJobs) {
      ExecutableJobEntity executableJobEntity = commandContext.jobFactory().getExecutableJob(jobEntity);
      commandContext.getExecutableJobEntityManager().insert(executableJobEntity);
      commandContext.getFailedJobEntityManager().delete(jobEntity);
    }
    return retriableJobs.size();
  }
}
