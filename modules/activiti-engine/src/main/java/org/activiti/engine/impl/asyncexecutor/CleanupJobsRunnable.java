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
package org.activiti.engine.impl.asyncexecutor;

import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.cmd.CleanupFailedJobsCommand;
import org.activiti.engine.impl.cmd.CleanupLockedJobsCommand;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.persistence.entity.ExecutableJobEntity;
import org.activiti.engine.impl.persistence.entity.LockedJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Vasile Dirla
 */
public class CleanupJobsRunnable implements Runnable {

  private static Logger log = LoggerFactory.getLogger(CleanupJobsRunnable.class);

  protected final AsyncExecutor asyncExecutor;

  protected volatile boolean isInterrupted;
  protected final Object MONITOR = new Object();
  protected final AtomicBoolean isWaiting = new AtomicBoolean(false);

  protected long millisToWait;
  protected int maxJobsPerAcquisition = 10;

  public CleanupJobsRunnable(AsyncExecutor asyncExecutor) {
    this.asyncExecutor = asyncExecutor;
  }

  public synchronized void run() {
    log.info("{} starting to move the timer jobs to the main job table /queue");

    final CommandExecutor commandExecutor = asyncExecutor.getCommandExecutor();

    while (!isInterrupted) {

      try {
        commandExecutor.execute(new CleanupFailedJobsCommand());

        final long maxLockDuration = 5 * 60 * 1000; // 5 minutes lock time
        CleanupLockedJobsCommand cleanupLockedJobsCommand = new CleanupLockedJobsCommand();
        cleanupLockedJobsCommand.setMaxLockDuration(maxLockDuration);

        commandExecutor.execute(cleanupLockedJobsCommand);

      } catch (Throwable e) {
        log.error("exception during timer job acquisition: {}", e.getMessage(), e);
        millisToWait = asyncExecutor.getDefaultTimerJobAcquireWaitTimeInMillis();
      }

      if (millisToWait > 0) {
        try {
          if (log.isDebugEnabled()) {
            log.debug("timer job acquisition thread sleeping for {} millis", millisToWait);
          }
          synchronized (MONITOR) {
            if (!isInterrupted) {
              isWaiting.set(true);
              MONITOR.wait(millisToWait);
            }
          }

          if (log.isDebugEnabled()) {
            log.debug("timer job acquisition thread woke up");
          }
        } catch (InterruptedException e) {
          if (log.isDebugEnabled()) {
            log.debug("timer job acquisition wait interrupted");
          }
        } finally {
          isWaiting.set(false);
        }
      }
    }

    log.info("{} stopped async job due acquisition");
  }

  public void stop() {
    synchronized (MONITOR) {
      isInterrupted = true;
      if (isWaiting.compareAndSet(true, false)) {
        MONITOR.notifyAll();
      }
    }
  }
}
