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
package org.activiti.engine.impl.bpmn.deployer;

import org.activiti.bpmn.model.EventDefinition;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.TimerEventDefinition;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.impl.cmd.CancelJobsCmd;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.jobexecutor.TimerEventHandler;
import org.activiti.engine.impl.jobexecutor.TimerStartEventJobHandler;
import org.activiti.engine.impl.persistence.entity.ExecutableTimerJobEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.persistence.entity.WaitingTimerJobEntity;
import org.activiti.engine.impl.util.CollectionUtil;
import org.activiti.engine.impl.util.TimerUtil;
import org.activiti.engine.runtime.Job;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages timers for newly-deployed process definitions and their previous versions.
 */
public class TimerManager {

  protected void removeObsoleteTimers(ProcessDefinitionEntity processDefinition) {
    List<Job> jobsToDelete = new ArrayList<Job>();


    if (processDefinition.getTenantId() != null && !ProcessEngineConfiguration.NO_TENANT_ID.equals(processDefinition.getTenantId())) {

      List<Job>  waitingTimerJobsToDelete = Context.getCommandContext().getWaitingTimerJobEntityManager()
              .findJobsByTypeAndProcessDefinitionKeyAndTenantId(TimerStartEventJobHandler.TYPE, processDefinition.getKey(), processDefinition.getTenantId());

      List<Job> executableJobsToDelete = Context.getCommandContext().getExecutableJobEntityManager()
              .findJobsByTypeAndProcessDefinitionKeyAndTenantId(TimerStartEventJobHandler.TYPE, processDefinition.getKey(), processDefinition.getTenantId());

      List<Job> lockedTimerJobsToDelete = Context.getCommandContext().getFailedJobEntityManager()
              .findJobsByTypeAndProcessDefinitionKeyAndTenantId(TimerStartEventJobHandler.TYPE, processDefinition.getKey(), processDefinition.getTenantId());

      List<Job> failedTimerJobsToDelete = Context.getCommandContext().getFailedJobEntityManager()
              .findJobsByTypeAndProcessDefinitionKeyAndTenantId(TimerStartEventJobHandler.TYPE, processDefinition.getKey(), processDefinition.getTenantId());

      if (failedTimerJobsToDelete != null) {
        jobsToDelete.addAll(failedTimerJobsToDelete);
      }
      if (lockedTimerJobsToDelete != null) {
        jobsToDelete.addAll(lockedTimerJobsToDelete);
      }
      if (executableJobsToDelete != null) {
        jobsToDelete.addAll(executableJobsToDelete);
      }
      if (waitingTimerJobsToDelete != null) {
        jobsToDelete.addAll(waitingTimerJobsToDelete);
      }

    } else {
      List<Job> waitingTimerJobsToDelete = Context.getCommandContext().getWaitingTimerJobEntityManager()
              .findJobsByTypeAndProcessDefinitionKeyNoTenantId(TimerStartEventJobHandler.TYPE, processDefinition.getKey());

      List<Job> executableJobsToDelete = Context.getCommandContext().getExecutableJobEntityManager()
              .findJobsByTypeAndProcessDefinitionKeyNoTenantId(TimerStartEventJobHandler.TYPE, processDefinition.getKey());

      List<Job> lockedTimerJobsToDelete = Context.getCommandContext().getFailedJobEntityManager()
              .findJobsByTypeAndProcessDefinitionKeyNoTenantId(TimerStartEventJobHandler.TYPE, processDefinition.getKey());

      List<Job> failedTimerJobsToDelete = Context.getCommandContext().getFailedJobEntityManager()
              .findJobsByTypeAndProcessDefinitionKeyNoTenantId(TimerStartEventJobHandler.TYPE, processDefinition.getKey());

      if (failedTimerJobsToDelete != null) {
        jobsToDelete.addAll(failedTimerJobsToDelete);
      }
      if (lockedTimerJobsToDelete != null) {
        jobsToDelete.addAll(lockedTimerJobsToDelete);
      }
      if (executableJobsToDelete != null) {
        jobsToDelete.addAll(executableJobsToDelete);
      }
      if (waitingTimerJobsToDelete != null) {
        jobsToDelete.addAll(waitingTimerJobsToDelete);
      }
    }

      for (Job job : jobsToDelete) {
        new CancelJobsCmd(job.getId()).execute(Context.getCommandContext());
      }

  }

  protected void scheduleTimers(ProcessDefinitionEntity processDefinition, Process process) {
    List<WaitingTimerJobEntity> timers = getTimerDeclarations(processDefinition, process);
    for (WaitingTimerJobEntity timer : timers) {
      Context.getCommandContext().getWaitingTimerJobEntityManager().insert(timer);
    }
  }

  protected List<WaitingTimerJobEntity> getTimerDeclarations(ProcessDefinitionEntity processDefinition, Process process) {
    List<WaitingTimerJobEntity> timers = new ArrayList<WaitingTimerJobEntity>();
    if (CollectionUtil.isNotEmpty(process.getFlowElements())) {
      for (FlowElement element : process.getFlowElements()) {
        if (element instanceof StartEvent) {
          StartEvent startEvent = (StartEvent) element;
          if (CollectionUtil.isNotEmpty(startEvent.getEventDefinitions())) {
            EventDefinition eventDefinition = startEvent.getEventDefinitions().get(0);
            if (eventDefinition instanceof TimerEventDefinition) {
              TimerEventDefinition timerEventDefinition = (TimerEventDefinition) eventDefinition;
              WaitingTimerJobEntity timer = TimerUtil
                      .createTimerEntityForTimerEventDefinition(timerEventDefinition, false, null, TimerStartEventJobHandler.TYPE,
                              TimerEventHandler.createConfiguration(startEvent.getId(), timerEventDefinition.getEndDate()));

              if (timer != null) {
                timer.setProcessDefinitionId(processDefinition.getId());

                if (processDefinition.getTenantId() != null) {
                  timer.setTenantId(processDefinition.getTenantId());
                }
                timers.add(timer);
              }

            }
          }
        }
      }
    }

    return timers;
  }
}

