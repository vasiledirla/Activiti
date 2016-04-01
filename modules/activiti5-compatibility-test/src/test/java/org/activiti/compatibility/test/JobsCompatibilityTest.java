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
package org.activiti.compatibility.test;

import org.activiti.engine.runtime.ProcessInstance;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JobsCompatibilityTest extends AbstractActiviti6CompatibilityTest {

  @Test
  public void testActiviti5JavaDelegate() {
    
    // Check data for existing process
     ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("jobTest").singleResult();
     assertNotNull(processInstance);

    managementService.createJobQuery().waitingTimers();

    Calendar calendar = Calendar.getInstance();
    Date baseTime = calendar.getTime();

    calendar.add(Calendar.MINUTE, 20);
    // expect to stop boundary jobs after 20 minutes
    DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
    DateTime dt = new DateTime(calendar.getTime());
    String dateStr = fmt.print(dt);

    // reset the timer
    Calendar nextTimeCal = Calendar.getInstance();
    nextTimeCal.setTime(baseTime);
    processEngineConfiguration.getClock().setCurrentTime(nextTimeCal.getTime());

     managementService.createJobQuery().locked();
   }

}
