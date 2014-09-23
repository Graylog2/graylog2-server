/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.alerts;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class AbstractAlertConditionTest extends AlertConditionTest {
    protected AlertCondition alertCondition;
    final protected int grace = 10;
    final protected int time = 10;

    @Override
    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();
        alertCondition = getDummyAlertCondition(getParametersMap(grace, time, 0));
    }

    @Test(enabled=false)
    public void testInGracePeriod() throws Exception {
        alertLastTriggered(-1);
        assertFalse("Should not be in grace period because alert was never fired", alertService.inGracePeriod(alertCondition));
        alertLastTriggered(0);
        assertTrue("Should be in grace period because alert was just fired", alertService.inGracePeriod(alertCondition));
        alertLastTriggered(grace * 60 - 1);
        assertTrue("Should be in grace period because alert was fired during grace period", alertService.inGracePeriod(alertCondition));
        alertLastTriggered(grace * 60 + 1);
        assertFalse("Should not be in grace period because alert was fired after grace period has passed", alertService.inGracePeriod(alertCondition));
        alertLastTriggered(Integer.MAX_VALUE);
        assertFalse("Should not be in grace period because alert was fired after grace period has passed", alertService.inGracePeriod(alertCondition));

        final AlertCondition alertConditionZeroGrace = getDummyAlertCondition(getParametersMap(0, time, 0));
        alertLastTriggered(0);
        assertFalse("Should not be in grace period because grace is zero", alertService.inGracePeriod(alertConditionZeroGrace));
        alertLastTriggered(-1);
        assertFalse("Should not be in grace period because grace is zero", alertService.inGracePeriod(alertConditionZeroGrace));
        alertLastTriggered(Integer.MAX_VALUE);
        assertFalse("Should not be in grace period because grace is zero", alertService.inGracePeriod(alertConditionZeroGrace));
    }

    protected AlertCondition getDummyAlertCondition(Map<String, Object> parameters) {
        return new AbstractAlertCondition(stream, CONDITION_ID, null, Tools.iso8601(), STREAM_CREATOR, parameters) {
            @Override
            public String getDescription() {
                return null;
            }

            @Override
            protected CheckResult runCheck() {
                return null;
            }

            @Override
            public List<Message> getSearchHits() {
                return null;
            }
        };
    }
}
