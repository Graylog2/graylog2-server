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
package org.graylog2.streams.matchers;

import org.graylog2.plugin.Message;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class AlwaysMatcherTest {
    @Test
    public void matchAlwaysReturnsTrue() throws Exception {
        final AlwaysMatcher matcher = new AlwaysMatcher();
        assertThat(matcher.match(null, null)).isTrue();
        assertThat(matcher.match(
                new Message("Test", "source", new DateTime(2016, 9, 7, 0, 0, DateTimeZone.UTC)),
                new StreamRuleMock(Collections.singletonMap("_id", "stream-rule-id"))))
                .isTrue();
    }

}