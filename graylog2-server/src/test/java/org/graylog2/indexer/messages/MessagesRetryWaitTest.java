/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.messages;

import com.github.rholder.retry.WaitStrategy;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MessagesRetryWaitTest {
    @Test
    public void secondsBasedRetryWaitsForSecondsStartingWith1() {
        WaitStrategy waitStrategy = Messages.exponentialWaitSeconds;
        assertThat(waitStrategy.computeSleepTime(new IndexBlockRetryAttempt(1))).isEqualTo(1000);
        assertThat(waitStrategy.computeSleepTime(new IndexBlockRetryAttempt(2))).isEqualTo(2000);
        assertThat(waitStrategy.computeSleepTime(new IndexBlockRetryAttempt(3))).isEqualTo(4000);
        assertThat(waitStrategy.computeSleepTime(new IndexBlockRetryAttempt(4))).isEqualTo(8000);
        assertThat(waitStrategy.computeSleepTime(new IndexBlockRetryAttempt(5))).isEqualTo(16000);
    }

    // This test was added to document how the retry strategy actually behaves since this is hard to deduct from the code
    @Test
    public void millisecondsBasedRetryWaitsForMillisecondsStartingWith2() {
        WaitStrategy waitStrategy = Messages.exponentialWaitMilliseconds;
        assertThat(waitStrategy.computeSleepTime(new IndexBlockRetryAttempt(1))).isEqualTo(2);
        assertThat(waitStrategy.computeSleepTime(new IndexBlockRetryAttempt(2))).isEqualTo(4);
        assertThat(waitStrategy.computeSleepTime(new IndexBlockRetryAttempt(3))).isEqualTo(8);
        assertThat(waitStrategy.computeSleepTime(new IndexBlockRetryAttempt(4))).isEqualTo(16);
        assertThat(waitStrategy.computeSleepTime(new IndexBlockRetryAttempt(5))).isEqualTo(32);
    }
}
