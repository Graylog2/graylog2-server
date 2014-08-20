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
package org.graylog2.inputs.extractors;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.testng.annotations.BeforeMethod;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public abstract class AbstractExtractorTest {
    protected MetricRegistry metricRegistry;
    protected Timer timer;
    protected Timer.Context timerContext;

    @BeforeMethod
    public void setUp() throws Exception {
        metricRegistry = mock(MetricRegistry.class);
        timer = mock(Timer.class);
        timerContext = mock(Timer.Context.class);
        when(metricRegistry.timer(anyString())).thenReturn(timer);
        when(timer.time()).thenReturn(timerContext);
    }
}
