package org.activiti.engine.impl.util;

import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.IntermediateCatchEvent;
import org.activiti.bpmn.model.TimerEventDefinition;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.VariableScope;
import org.activiti.engine.impl.calendar.BusinessCalendar;
import org.activiti.engine.impl.calendar.CycleBusinessCalendar;
import org.activiti.engine.impl.calendar.DueDateBusinessCalendar;
import org.activiti.engine.impl.calendar.DurationBusinessCalendar;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.el.ExpressionManager;
import org.activiti.engine.impl.el.NoExecutionVariableScope;
import org.activiti.engine.impl.persistence.entity.ExecutableTimerJobEntity;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Joram Barrez
 */
public class TimerUtil {

  /**
   * The event definition on which the timer is based.
   * 
   * Takes in an optional execution, if missing the {@link NoExecutionVariableScope} will be used (eg Timer start event)
   */
  public static ExecutableTimerJobEntity createTimerEntityForTimerEventDefinition(TimerEventDefinition timerEventDefinition, boolean isInterruptingTimer,
      ExecutionEntity executionEntity, String jobHandlerType, String jobHandlerConfig) {

    String businessCalendarRef = null;
    Expression endDateExpression = null;
    Expression expression = null;

    ExpressionManager expressionManager = Context.getProcessEngineConfiguration().getExpressionManager();
    if (StringUtils.isNotEmpty(timerEventDefinition.getTimeDate())) {

      businessCalendarRef = DueDateBusinessCalendar.NAME;
      expression = expressionManager.createExpression(timerEventDefinition.getTimeDate());

    } else if (StringUtils.isNotEmpty(timerEventDefinition.getTimeCycle())) {

      businessCalendarRef = CycleBusinessCalendar.NAME;
      expression = expressionManager.createExpression(timerEventDefinition.getTimeCycle());

    } else if (StringUtils.isNotEmpty(timerEventDefinition.getTimeDuration())) {

      businessCalendarRef = DurationBusinessCalendar.NAME;
      expression = expressionManager.createExpression(timerEventDefinition.getTimeDuration());

    }

    if (expression == null) {
      throw new ActivitiException("Timer needs configuration (either timeDate, timeCycle or timeDuration is needed) (" + timerEventDefinition.getId() + ")");
    }

    BusinessCalendar businessCalendar = Context.getProcessEngineConfiguration().getBusinessCalendarManager().getBusinessCalendar(businessCalendarRef);

    String dueDateString = null;
    Date dueDate = null;

    // ACT-1415: timer-declaration on start-event may contain expressions NOT
    // evaluating variables but other context, evaluating should happen nevertheless
    VariableScope scopeForExpression = executionEntity;
    if (scopeForExpression == null) {
      scopeForExpression = NoExecutionVariableScope.getSharedInstance();
    }

    Object dueDateValue = expression.getValue(scopeForExpression);
    if (dueDateValue instanceof String) {
      dueDateString = (String) dueDateValue;
    } else if (dueDateValue instanceof Date) {
      dueDate = (Date) dueDateValue;
    } else if (dueDateValue instanceof DateTime) {
      //JodaTime support
      dueDate = ((DateTime) dueDateValue).toDate();
    } else if(dueDateValue!=null){
      throw new ActivitiException("Timer '" + executionEntity.getActivityId()
          + "' was not configured with a valid duration/time, either hand in a java.util.Date or a String in format 'yyyy-MM-dd'T'hh:mm:ss'");
    }
    //dueDateValue==null is OK - but unexpected class type must throw an error.

    if (dueDate == null && dueDateString != null) {
      dueDate = businessCalendar.resolveDuedate(dueDateString);
    }

    if (StringUtils.isNotEmpty(timerEventDefinition.getEndDate())){
      endDateExpression = expressionManager.createExpression(timerEventDefinition.getEndDate());
    }

    String endDateString = null;
    Date endDate = null;

    if (endDateExpression != null) {
      Object endDateValue = endDateExpression.getValue(scopeForExpression);
      if (dueDateValue instanceof String) {
        endDateString = (String) endDateValue;
      } else if (dueDateValue instanceof Date) {
        endDate = (Date) endDateValue;
      } else if (dueDateValue instanceof DateTime) {
        //JodaTime support
        endDate = ((DateTime) endDateValue).toDate();
      } else if (endDateValue != null) {
        throw new ActivitiException("Timer '" + executionEntity.getActivityId()
                + "' was not configured with a valid endDate, either hand in a java.util.Date or a String in format 'yyyy-MM-dd'T'hh:mm:ss'");
      }

      if (endDate == null && endDateString != null) {
        endDate = businessCalendar.resolveEndDate(endDateString);
      }
    }

    ExecutableTimerJobEntity timer = null;
    if (dueDate != null) {
      timer = Context.getCommandContext().getExecutableJobEntityManager().createTimer();
      timer.setJobHandlerType(jobHandlerType);
      timer.setJobHandlerConfiguration(jobHandlerConfig);
      timer.setExclusive(true);
      timer.setRetries(TimerEntity.DEFAULT_RETRIES);
      timer.setDuedate(dueDate);

      if (endDate !=null) {
        timer.setEndDate(endDate);
      }

      if (executionEntity != null) {
        timer.setExecution(executionEntity);
        timer.setProcessDefinitionId(executionEntity.getProcessDefinitionId());
        timer.setProcessInstanceId(executionEntity.getProcessInstanceId());

        // Inherit tenant identifier (if applicable)
        if (executionEntity.getTenantId() != null) {
          timer.setTenantId(executionEntity.getTenantId());
        }
      }
    }

    if (StringUtils.isNotEmpty(timerEventDefinition.getTimeCycle())) {
      // See ACT-1427: A boundary timer with a cancelActivity='true', doesn't need to repeat itself
      boolean repeat = !isInterruptingTimer;

      // ACT-1951: intermediate catching timer events shouldn't repeat according to spec
      if (executionEntity != null) {
        FlowElement currentElement = executionEntity.getCurrentFlowElement();
        if (currentElement != null && currentElement instanceof IntermediateCatchEvent) {
          repeat = false;
        }
      }

      if (repeat) {
        String prepared = prepareRepeat(dueDateString);
        timer.setRepeat(prepared);
      }
    }

    if (timer != null && executionEntity != null) {
      timer.setExecution(executionEntity);
      timer.setProcessDefinitionId(executionEntity.getProcessDefinitionId());

      // Inherit tenant identifier (if applicable)
      if (executionEntity != null && executionEntity.getTenantId() != null) {
        timer.setTenantId(executionEntity.getTenantId());
      }
    }

    return timer;
  }

  public static String prepareRepeat(String dueDate) {
    if (dueDate.startsWith("R") && dueDate.split("/").length == 2) {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
      return dueDate.replace("/", "/" + sdf.format(Context.getProcessEngineConfiguration().getClock().getCurrentTime()) + "/");
    }
    return dueDate;
  }

}
