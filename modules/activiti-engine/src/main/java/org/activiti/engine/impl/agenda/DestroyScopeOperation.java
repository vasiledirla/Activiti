package org.activiti.engine.impl.agenda;

import java.util.Collection;

import org.activiti.bpmn.model.FlowElement;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutableJobEntity;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ExecutionEntityManager;
import org.activiti.engine.impl.persistence.entity.FailedJobEntity;
import org.activiti.engine.impl.persistence.entity.FailedJobEntityManager;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.ExecutableJobEntityManager;
import org.activiti.engine.impl.persistence.entity.LockedJobEntity;
import org.activiti.engine.impl.persistence.entity.LockedJobEntityManager;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntityManager;
import org.activiti.engine.impl.persistence.entity.VariableInstanceEntity;
import org.activiti.engine.impl.persistence.entity.VariableInstanceEntityManager;

/**
 * @author Joram Barrez
 */
public class DestroyScopeOperation extends AbstractOperation {

  public DestroyScopeOperation(CommandContext commandContext, ExecutionEntity execution) {
    super(commandContext, execution);
  }

  @Override
  public void run() {

    FlowElement currentFlowElement = execution.getCurrentFlowElement();

    // Find the actual scope that needs to be destroyed.
    // This could be the incoming execution, or the first parent execution where isScope = true

    // Find parent scope execution
    ExecutionEntityManager executionEntityManager = commandContext.getExecutionEntityManager();
    ExecutionEntity executionEntity = (ExecutionEntity) execution;
    ExecutionEntity parentScopeExecution = null;

    if (execution.isScope()) {
      parentScopeExecution = executionEntity;
    } else {
      ExecutionEntity currentlyExaminedExecution = executionEntityManager.findById(execution.getParentId());
      while (currentlyExaminedExecution != null && parentScopeExecution == null) {
        if (currentlyExaminedExecution.isScope()) {
          parentScopeExecution = currentlyExaminedExecution;
        } else {
          currentlyExaminedExecution = executionEntityManager.findById(currentlyExaminedExecution.getParentId());
        }
      }
    }

    if (parentScopeExecution == null) {
      throw new ActivitiException("Programmatic error: no parent scope execution found for boundary event");
    }

    // Delete all child executions
    Collection<ExecutionEntity> childExecutions = executionEntityManager.findChildExecutionsByParentExecutionId(parentScopeExecution.getId());
    for (ExecutionEntity childExecution : childExecutions) {
      executionEntityManager.deleteExecutionAndRelatedData(childExecution, null, false);
    }

    // Delete all scope tasks
    TaskEntityManager taskEntityManager = commandContext.getTaskEntityManager();
    Collection<TaskEntity> tasksForExecution = taskEntityManager.findTasksByExecutionId(parentScopeExecution.getId());
    for (TaskEntity taskEntity : tasksForExecution) {
      taskEntityManager.delete(taskEntity);
    }

    // Delete all scope jobs

    // executable jobs
    ExecutableJobEntityManager executableJobEntityManager = commandContext.getExecutableJobEntityManager();
    Collection<ExecutableJobEntity> executableJobsForExecution = executableJobEntityManager.findJobsByExecutionId(parentScopeExecution.getId());
    for (ExecutableJobEntity job : executableJobsForExecution) {
      executableJobEntityManager.delete(job);
    }

    // locked jobs
    LockedJobEntityManager lockedJobEntityManager = commandContext.getLockedJobEntityManager();
    Collection<LockedJobEntity> lockedJobsForExecution = lockedJobEntityManager.findJobsByExecutionId(parentScopeExecution.getId());
    for (LockedJobEntity job : lockedJobsForExecution) {
      lockedJobEntityManager.delete(job);
    }

    // Failed jobs
    FailedJobEntityManager failedJobEntityManager = commandContext.getFailedJobEntityManager();
    Collection<FailedJobEntity> failedJobsForExecution = failedJobEntityManager.findJobsByExecutionId(parentScopeExecution.getId());
    for (FailedJobEntity job : failedJobsForExecution) {
      failedJobEntityManager.delete(job);
    }

    
    // Remove variables associated with this scope
    VariableInstanceEntityManager variableInstanceEntityManager = commandContext.getVariableInstanceEntityManager();
    Collection<VariableInstanceEntity> variablesForExecution = variableInstanceEntityManager.findVariableInstancesByExecutionId(parentScopeExecution.getId());
    for (VariableInstanceEntity variable : variablesForExecution) {
      variableInstanceEntityManager.delete(variable);
    }

    // Not a scope anymore
    executionEntityManager.delete(parentScopeExecution);
  }

}
