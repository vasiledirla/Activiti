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
package org.activiti.engine.impl;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.JobNotFoundException;
import org.activiti.engine.ManagementService;
import org.activiti.engine.event.EventLogEntry;
import org.activiti.engine.impl.cmd.CustomSqlExecution;
import org.activiti.engine.impl.cmd.DeleteEventLogEntry;
import org.activiti.engine.impl.cmd.ExecuteCustomSqlCmd;
import org.activiti.engine.impl.cmd.GetEventLogEntriesCmd;
import org.activiti.engine.impl.cmd.GetPropertiesCmd;
import org.activiti.engine.impl.cmd.GetTableCountCmd;
import org.activiti.engine.impl.cmd.GetTableMetaDataCmd;
import org.activiti.engine.impl.cmd.GetTableNameCmd;
import org.activiti.engine.impl.cmd.jobs.DeleteAsyncJobCmd;
import org.activiti.engine.impl.cmd.jobs.DeleteJobCmd;
import org.activiti.engine.impl.cmd.jobs.DeleteTimerJobCmd;
import org.activiti.engine.impl.cmd.jobs.ExecuteAsyncJobsCmd;
import org.activiti.engine.impl.cmd.jobs.ExecuteJobsCmd;
import org.activiti.engine.impl.cmd.jobs.ExecuteTimerJobsCmd;
import org.activiti.engine.impl.cmd.jobs.GetAsyncJobExceptionStacktraceCmd;
import org.activiti.engine.impl.cmd.jobs.GetTimerJobExceptionStacktraceCmd;
import org.activiti.engine.impl.cmd.jobs.SetAsyncJobRetriesCmd;
import org.activiti.engine.impl.cmd.jobs.SetTimerJobRetriesCmd;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.db.DbSqlSession;
import org.activiti.engine.impl.db.DbSqlSessionFactory;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandConfig;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.jobexecutor.JobExecutorContext;
import org.activiti.engine.impl.jobexecutor.SingleJobExecutorContext;
import org.activiti.engine.impl.persistence.entity.MessageEntity;
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.activiti.engine.management.TableMetaData;
import org.activiti.engine.management.TablePageQuery;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.JobQuery;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Falko Menge
 * @author Saeid Mizaei
 */
public class ManagementServiceImpl extends ServiceImpl implements ManagementService {

  public Map<String, Long> getTableCount() {
    return commandExecutor.execute(new GetTableCountCmd());
  }

  public String getTableName(Class<?> activitiEntityClass) {
    return commandExecutor.execute(new GetTableNameCmd(activitiEntityClass));
  }

  public TableMetaData getTableMetaData(String tableName) {
    return commandExecutor.execute(new GetTableMetaDataCmd(tableName));
  }

  @Override
  public void executeJob(Job job) {

    if (job == null) {
      throw new ActivitiIllegalArgumentException("job is null");
    }

    JobExecutorContext jobExecutorContext = new SingleJobExecutorContext();
    Context.setJobExecutorContext(jobExecutorContext);
    try {
      if (job instanceof MessageEntity) {
        commandExecutor.execute(new ExecuteAsyncJobsCmd(job.getId()));
      } else if (job instanceof TimerEntity) {
        commandExecutor.execute(new ExecuteTimerJobsCmd(job.getId()));
      }
    }
    catch (RuntimeException e) {
      if ((e instanceof JobNotFoundException)) {
        throw e;
      } else {
        throw new ActivitiException("Job " + job.getId() + " failed", e);
      }
    } finally {
      Context.removeJobExecutorContext();
    }
  }

  public void executeTimerJob(String jobId) {

    if (jobId == null) {
      throw new ActivitiIllegalArgumentException("JobId is null");
    }

    JobExecutorContext jobExecutorContext = new SingleJobExecutorContext();
    Context.setJobExecutorContext(jobExecutorContext);
    try {
      commandExecutor.execute(new ExecuteTimerJobsCmd(jobId));
    } catch (RuntimeException e) {
      if ((e instanceof JobNotFoundException)) {
        throw e;
      } else {
        throw new ActivitiException("Job " + jobId + " failed", e);
      }
    } finally {
      Context.removeJobExecutorContext();
    }
  }

  public void executeAsyncJob(String jobId) {

    if (jobId == null) {
      throw new ActivitiIllegalArgumentException("JobId is null");
    }

    JobExecutorContext jobExecutorContext = new SingleJobExecutorContext();
    Context.setJobExecutorContext(jobExecutorContext);
    try {
      commandExecutor.execute(new ExecuteAsyncJobsCmd(jobId));
    } catch (RuntimeException e) {
      if ((e instanceof JobNotFoundException)) {
        throw e;
      } else {
        throw new ActivitiException("Job " + jobId + " failed", e);
      }
    } finally {
      Context.removeJobExecutorContext();
    }
  }


  public void executeJob(String jobId) {
    if (jobId == null) {
      throw new ActivitiIllegalArgumentException("JobId is null");
    }

    JobExecutorContext jobExecutorContext = new SingleJobExecutorContext();
    Context.setJobExecutorContext(jobExecutorContext);
    try {
      commandExecutor.execute(new ExecuteJobsCmd(jobId));
    } catch (RuntimeException e) {
      if ((e instanceof JobNotFoundException)) {
        throw e;
      } else {
        throw new ActivitiException("Job " + jobId + " failed", e);
      }
    } finally {
      Context.removeJobExecutorContext();
    }
  }

  public void deleteJob(Job job) {
    if (job == null) {
      throw new ActivitiIllegalArgumentException("Job is null");
    }

    if (job instanceof MessageEntity) {
      commandExecutor.execute(new DeleteAsyncJobCmd(job));
    } else if (job instanceof TimerEntity) {
      commandExecutor.execute(new DeleteTimerJobCmd(job));
    }
  }

  public void deleteJob(String jobId) {
    if (jobId == null) {
      throw new ActivitiIllegalArgumentException("jobId is null");
    }

    commandExecutor.execute(new DeleteJobCmd(jobId));
  }

  public void deleteAsyncJob(String jobId) {
    commandExecutor.execute(new DeleteAsyncJobCmd(jobId));
  }

  public void deleteTimerJob(String jobId) {
    commandExecutor.execute(new DeleteTimerJobCmd(jobId));
  }

  public void setAsyncJobRetries(String jobId, int retries) {
    commandExecutor.execute(new SetAsyncJobRetriesCmd(jobId, retries));
  }

  public void setTimerJobRetries(String jobId, int retries) {
    commandExecutor.execute(new SetTimerJobRetriesCmd(jobId, retries));
  }

  public void setJobRetries(Job job, int retries) {
    if (job instanceof MessageEntity) {
      commandExecutor.execute(new SetAsyncJobRetriesCmd(job, retries));
    } else if (job instanceof TimerEntity) {
      commandExecutor.execute(new SetTimerJobRetriesCmd(job, retries));
    }
  }

  public TablePageQuery createTablePageQuery() {
    return new TablePageQueryImpl(commandExecutor);
  }

  public JobQuery createJobQuery() {
    return new JobQueryImpl(commandExecutor);
  }

  public String getAsyncJobExceptionStacktrace(String jobId) {
    return commandExecutor.execute(new GetAsyncJobExceptionStacktraceCmd(jobId));
  }

  public String getTimerJobExceptionStacktrace(String jobId) {
    return commandExecutor.execute(new GetTimerJobExceptionStacktraceCmd(jobId));
  }

  public String getJobExceptionStacktrace(Job job) {
    if (job == null) {
      throw new ActivitiIllegalArgumentException("job is null");
    }
    if (job instanceof MessageEntity) {
      return getAsyncJobExceptionStacktrace(job.getId());
    } else if (job instanceof TimerEntity) {
      return getTimerJobExceptionStacktrace(job.getId());
    }
    return null;
  }

  public Map<String, String> getProperties() {
    return commandExecutor.execute(new GetPropertiesCmd());
  }

  public String databaseSchemaUpgrade(final Connection connection, final String catalog, final String schema) {
    CommandConfig config = commandExecutor.getDefaultConfig().transactionNotSupported();
    return commandExecutor.execute(config, new Command<String>() {

      public String execute(CommandContext commandContext) {
        DbSqlSessionFactory dbSqlSessionFactory = (DbSqlSessionFactory) commandContext.getSessionFactories().get(DbSqlSession.class);
        DbSqlSession dbSqlSession = new DbSqlSession(dbSqlSessionFactory, commandContext.getEntityCache(), connection, catalog, schema);
        commandContext.getSessions().put(DbSqlSession.class, dbSqlSession);
        return dbSqlSession.dbSchemaUpdate();
      }
    });
  }

  public <T> T executeCommand(Command<T> command) {
    if (command == null) {
      throw new ActivitiIllegalArgumentException("The command is null");
    }
    return commandExecutor.execute(command);
  }

  public <T> T executeCommand(CommandConfig config, Command<T> command) {
    if (config == null) {
      throw new ActivitiIllegalArgumentException("The config is null");
    }
    if (command == null) {
      throw new ActivitiIllegalArgumentException("The command is null");
    }
    return commandExecutor.execute(config, command);
  }

  @Override
  public <MapperType, ResultType> ResultType executeCustomSql(CustomSqlExecution<MapperType, ResultType> customSqlExecution) {
    Class<MapperType> mapperClass = customSqlExecution.getMapperClass();
    return commandExecutor.execute(new ExecuteCustomSqlCmd<MapperType, ResultType>(mapperClass, customSqlExecution));
  }

  @Override
  public List<EventLogEntry> getEventLogEntries(Long startLogNr, Long pageSize) {
    return commandExecutor.execute(new GetEventLogEntriesCmd(startLogNr, pageSize));
  }

  @Override
  public List<EventLogEntry> getEventLogEntriesByProcessInstanceId(String processInstanceId) {
    return commandExecutor.execute(new GetEventLogEntriesCmd(processInstanceId));
  }

  @Override
  public void deleteEventLogEntry(long logNr) {
    commandExecutor.execute(new DeleteEventLogEntry(logNr));
  }

}
