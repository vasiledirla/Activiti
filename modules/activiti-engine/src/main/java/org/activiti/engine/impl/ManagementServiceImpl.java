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
import org.activiti.engine.impl.cmd.*;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.db.DbSqlSession;
import org.activiti.engine.impl.db.DbSqlSessionFactory;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandConfig;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.jobexecutor.JobExecutorContext;
import org.activiti.engine.impl.jobexecutor.SingleJobExecutorContext;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.TimerJobEntityImpl;
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
      commandExecutor.execute(new ExecuteJobsCmd(job.getJobType(), job.getId()));
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
      commandExecutor.execute(new ExecuteJobsCmd(Job.TIMER, jobId));
    }
    catch (RuntimeException e) {
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
      commandExecutor.execute(new ExecuteJobsCmd(Job.MESSAGE, jobId));
    }
    catch (RuntimeException e) {
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
    commandExecutor.execute(new DeleteJobCmd(job));
  }

  public void deleteAsyncJob(String jobId) {
    commandExecutor.execute(new DeleteJobCmd(Job.MESSAGE, jobId));
  }

  public void deleteTimerJob(String jobId) {
    commandExecutor.execute(new DeleteJobCmd(Job.TIMER, jobId));
  }

  public void setAsyncJobRetries(String jobId, int retries) {
    commandExecutor.execute(new SetJobRetriesCmd(Job.MESSAGE, jobId, retries));
  }

  public void setTimerJobRetries(String jobId, int retries) {
    commandExecutor.execute(new SetJobRetriesCmd(Job.TIMER, jobId, retries));
  }

  public void setJobRetries(Job job, int retries) {
    commandExecutor.execute(new SetJobRetriesCmd(job, retries));
  }

  public TablePageQuery createTablePageQuery() {
    return new TablePageQueryImpl(commandExecutor);
  }

  public JobQuery createJobQuery() {
    return new JobQueryImpl(commandExecutor);
  }

  public String getAsyncJobExceptionStacktrace(String jobId) {
    return commandExecutor.execute(new GetJobExceptionStacktraceCmd(Job.MESSAGE, jobId));
  }

  public String getTimerJobExceptionStacktrace(String jobId) {
    return commandExecutor.execute(new GetJobExceptionStacktraceCmd(Job.TIMER, jobId));
  }

  public String getJobExceptionStacktrace(Job job) {
    if (job == null) {
      throw new ActivitiIllegalArgumentException("job is null");
    }
    return commandExecutor.execute(new GetJobExceptionStacktraceCmd(job.getJobType(), job.getId()));
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
