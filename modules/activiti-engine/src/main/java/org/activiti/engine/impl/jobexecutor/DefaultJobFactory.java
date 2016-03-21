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

import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.persistence.entity.ExecutableJobEntity;
import org.activiti.engine.impl.persistence.entity.ExecutableMessageJobEntityImpl;
import org.activiti.engine.impl.persistence.entity.ExecutableTimerJobEntityImpl;
import org.activiti.engine.impl.persistence.entity.FailedJobEntity;
import org.activiti.engine.impl.persistence.entity.FailedMessageJobEntityImpl;
import org.activiti.engine.impl.persistence.entity.FailedTimerJobEntityImpl;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.LockedJobEntity;
import org.activiti.engine.impl.persistence.entity.LockedMessageJobEntity;
import org.activiti.engine.impl.persistence.entity.LockedMessageJobEntityImpl;
import org.activiti.engine.impl.persistence.entity.LockedTimerJobEntity;
import org.activiti.engine.impl.persistence.entity.LockedTimerJobEntityImpl;
import org.activiti.engine.impl.persistence.entity.MessageEntity;
import org.activiti.engine.impl.persistence.entity.TimerEntity;

import java.util.Date;

/**
 * @author Vasile Dirla
 */
public class DefaultJobFactory implements JobFactory {

  @Override
  public FailedJobEntity getFailedJob(LockedJobEntity lockedJobEntity) {
    if (lockedJobEntity instanceof MessageEntity) {
      return new FailedMessageJobEntityImpl((LockedMessageJobEntity) lockedJobEntity);
    } else if (lockedJobEntity instanceof TimerEntity) {
      return new FailedTimerJobEntityImpl((LockedTimerJobEntity) lockedJobEntity);
    }
    throw new ActivitiException("unknown job type: " + lockedJobEntity.getClass().getName());
  }
  @Override
  public ExecutableJobEntity getExecutableJob(JobEntity jobEntity) {
    if (jobEntity instanceof MessageEntity) {
      return new ExecutableMessageJobEntityImpl((MessageEntity) jobEntity);
    } else if (jobEntity instanceof TimerEntity) {
      return new ExecutableTimerJobEntityImpl((TimerEntity) jobEntity);
    }
    throw new ActivitiException("unknown job type: " + jobEntity.getClass().getName());
  }

  @Override
  public LockedJobEntity getLockedJob(ExecutableJobEntity jobEntity, Date time, String lockOwner) {
    if (jobEntity instanceof MessageEntity) {
      return new LockedMessageJobEntityImpl((MessageEntity) jobEntity, time, lockOwner);
    } else if (jobEntity instanceof TimerEntity) {
      return new LockedTimerJobEntityImpl((TimerEntity) jobEntity, time, lockOwner);
    }
    throw new ActivitiException("unknown job type: " + jobEntity.getClass().getName());
  }
}
