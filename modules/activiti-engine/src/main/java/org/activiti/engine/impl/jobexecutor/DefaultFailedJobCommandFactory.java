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
package org.activiti.engine.impl.jobexecutor;

import org.activiti.engine.impl.cmd.jobs.AsyncJobRetryCmd;
import org.activiti.engine.impl.cmd.jobs.TimerJobRetryCmd;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.runtime.Job;

/**
 * @author Saeid Mirzaei
 * @author Vasile Dirla
 */
public class DefaultFailedJobCommandFactory implements FailedJobCommandFactory {

  String TIMER = "timer";
  String MESSAGE = "message";

  @Override
  public Command<Object> getCommand(String jobType, String jobId, Throwable exception) {
    if (jobType.equalsIgnoreCase(MESSAGE)) {
      return new AsyncJobRetryCmd(jobId, exception);
    } else if (jobType.equalsIgnoreCase(TIMER)) {
      return new TimerJobRetryCmd(jobId, exception);
    }
    return null;
  }

}
