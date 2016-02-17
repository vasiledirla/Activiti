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

package org.activiti.engine.impl.persistence;

import org.activiti.engine.delegate.event.ActivitiEventDispatcher;
import org.activiti.engine.impl.asyncexecutor.AsyncExecutor;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.history.HistoryManager;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.activiti.engine.impl.persistence.entity.*;
import org.activiti.engine.runtime.Clock;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public abstract class AbstractManager {
  
  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  
  public AbstractManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
    this.processEngineConfiguration = processEngineConfiguration;
  }
  
  // Command scoped 
  
  protected CommandContext getCommandContext() {
    return Context.getCommandContext();
  }

  protected <T> T getSession(Class<T> sessionClass) {
    return getCommandContext().getSession(sessionClass);
  }
  
  // Engine scoped
  
  protected ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
    return processEngineConfiguration;
  }
  
  protected CommandExecutor getCommandExecutor() {
    return getProcessEngineConfiguration().getCommandExecutor();
  }
  
  protected Clock getClock() {
    return getProcessEngineConfiguration().getClock();
  }
  
  protected AsyncExecutor getAsyncExecutor() {
    return getProcessEngineConfiguration().getAsyncExecutor();
  }
  
  protected JobExecutor getJobExecutor() {
    return getProcessEngineConfiguration().getJobExecutor();
  }
  
  protected ActivitiEventDispatcher getEventDispatcher() {
    return getProcessEngineConfiguration().getEventDispatcher();
  }
  
  protected HistoryManager getHistoryManager() {
    return getProcessEngineConfiguration().getHistoryManager();
  }

  protected DeploymentEntityManager getDeploymentEntityManager() {
    return getProcessEngineConfiguration().getDeploymentEntityManager();
  }

  protected ResourceEntityManager getResourceEntityManager() {
    return getProcessEngineConfiguration().getResourceEntityManager();
  }

  protected ByteArrayEntityManager getByteArrayEntityManager() {
    return getProcessEngineConfiguration().getByteArrayEntityManager();
  }

  protected ProcessDefinitionEntityManager getProcessDefinitionEntityManager() {
    return getProcessEngineConfiguration().getProcessDefinitionEntityManager();
  }
  
  protected ProcessDefinitionInfoEntityManager getProcessDefinitionInfoEntityManager() {
    return getProcessEngineConfiguration().getProcessDefinitionInfoEntityManager();
  }

  protected ModelEntityManager getModelEntityManager() {
    return getProcessEngineConfiguration().getModelEntityManager();
  }

  protected ExecutionEntityManager getExecutionEntityManager() {
    return getProcessEngineConfiguration().getExecutionEntityManager();
  }

  protected TaskEntityManager getTaskEntityManager() {
    return getProcessEngineConfiguration().getTaskEntityManager();
  }

  protected IdentityLinkEntityManager getIdentityLinkEntityManager() {
    return getProcessEngineConfiguration().getIdentityLinkEntityManager();
  }

  protected EventSubscriptionEntityManager getEventSubscriptionEntityManager() {
    return getProcessEngineConfiguration().getEventSubscriptionEntityManager();
  }

  protected VariableInstanceEntityManager getVariableInstanceEntityManager() {
    return getProcessEngineConfiguration().getVariableInstanceEntityManager();
  }

  protected HistoricProcessInstanceEntityManager getHistoricProcessInstanceEntityManager() {
    return getProcessEngineConfiguration().getHistoricProcessInstanceEntityManager();
  }

  protected HistoricDetailEntityManager getHistoricDetailEntityManager() {
    return getProcessEngineConfiguration().getHistoricDetailEntityManager();
  }

  protected HistoricActivityInstanceEntityManager getHistoricActivityInstanceEntityManager() {
    return getProcessEngineConfiguration().getHistoricActivityInstanceEntityManager();
  }

  protected HistoricVariableInstanceEntityManager getHistoricVariableInstanceEntityManager() {
    return getProcessEngineConfiguration().getHistoricVariableInstanceEntityManager();
  }

  protected HistoricTaskInstanceEntityManager getHistoricTaskInstanceEntityManager() {
    return getProcessEngineConfiguration().getHistoricTaskInstanceEntityManager();
  }

  protected HistoricIdentityLinkEntityManager getHistoricIdentityLinkEntityManager() {
    return getProcessEngineConfiguration().getHistoricIdentityLinkEntityManager();
  }

  protected UserEntityManager getUserIdentityEntityManager() {
    return getProcessEngineConfiguration().getUserEntityManager();
  }

  protected GroupEntityManager getGroupEntityManager() {
    return getProcessEngineConfiguration().getGroupEntityManager();
  }

  protected IdentityInfoEntityManager getIdentityInfoEntityManager() {
    return getProcessEngineConfiguration().getIdentityInfoEntityManager();
  }

  protected MembershipEntityManager getMembershipEntityManager() {
    return getProcessEngineConfiguration().getMembershipEntityManager();
  }

  protected AttachmentEntityManager getAttachmentEntityManager() {
    return getProcessEngineConfiguration().getAttachmentEntityManager();
  }
  
  protected CommentEntityManager getCommentEntityManager() {
    return getProcessEngineConfiguration().getCommentEntityManager();
  }
  
  protected TimerJobEntityManager getTimerJobEntityManager() {
    return getProcessEngineConfiguration().getTimerJobEntityManager();
  }

  protected AsyncJobEntityManager getAsyncJobEntityManager() {
    return getProcessEngineConfiguration().getAsyncJobEntityManager();
  }
  protected GenericJobEntityManager getGenericJobEntityManager() {
    return getProcessEngineConfiguration().getGenericJobEntityManager();
  }

}
