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
package org.graylog2.auditlog;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;

import static org.assertj.core.api.Assertions.assertThat;

public class StdOutAppenderTest {
    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    private StdOutAppender appender;

    @Before
    public void setUp() throws Exception {
        appender = new StdOutAppender(true);
    }

    @Test
    public void enabledReturnsConfigurationValue() {
        assertThat(new StdOutAppender(true).enabled()).isTrue();
        assertThat(new StdOutAppender(false).enabled()).isFalse();
    }

    @Test
    public void writesTextToSystemOut() {
        final ImmutableMap<String, Object> context = ImmutableMap.of(
            "string", "foobar",
            "number", 1L,
            "boolean", true);

        appender.write(SuccessStatus.SUCCESS, "subject", "action", "object", context);

        assertThat(systemOutRule.getLogWithNormalizedLineSeparator())
            .startsWith("# AUDIT LOG ENTRY")
            .contains("Status=SUCCESS\n")
            .contains("Timestamp=")
            .containsSequence("Subject=subject\n" +
                "Action=action\n" +
                "Object=object\n" +
                "Context=string:foobar,number:1,boolean:true"
            );
    }
}
