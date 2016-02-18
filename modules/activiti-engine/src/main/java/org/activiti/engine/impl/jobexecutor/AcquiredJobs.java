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

import org.activiti.engine.impl.persistence.entity.JobEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Tom Baeyens
 * @author Daniel Meyer
 */
public class AcquiredJobs {

  protected List<List<JobEntity>> acquiredJobBatches = new ArrayList<List<JobEntity>>();
  protected Set<JobEntity> acquiredJobs = new HashSet<JobEntity>();
  protected Set<String> acquiredJobIds = new HashSet<String>();

  public List<List<JobEntity>> getJobIdBatches() {
    return acquiredJobBatches;
  }

  public void addJobBatch(List<JobEntity> jobs) {
    acquiredJobBatches.add(jobs);
    acquiredJobs.addAll(jobs);

    for (JobEntity jobEntity : jobs) {
      acquiredJobIds.add(jobEntity.getId());
    }

  }

  public boolean contains(JobEntity job) {
    return acquiredJobIds.contains(job.getId());
  }

  public int size() {
    return acquiredJobs.size();
  }

}
