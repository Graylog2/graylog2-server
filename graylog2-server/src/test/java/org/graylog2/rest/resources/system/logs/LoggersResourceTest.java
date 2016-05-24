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
package org.graylog2.rest.resources.system.logs;

import org.apache.logging.log4j.Level;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class LoggersResourceTest {
    private LoggersResource resource;

    public LoggersResourceTest() {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    @Before
    public void setUp() throws Exception {
        resource = new LoggersResource();
    }

    @Test
    public void setLoggerLevelOnlySetsLoggersLevel() throws Exception {
        final String parentLoggerName = "LoggersResourceTest";
        final String loggerName = "LoggersResourceTest.setLoggerLevelOnlySetsLoggersLevel";
        final Level originalLevel = resource.getLoggerLevel(loggerName);
        final Level parentLevel = resource.getLoggerLevel(parentLoggerName);
        assertThat(originalLevel).isEqualTo(parentLevel);
        final Level expectedLevel = Level.TRACE;
        assertThat(originalLevel).isNotEqualTo(expectedLevel);

        resource.setLoggerLevel(loggerName, expectedLevel);
        assertThat(resource.getLoggerLevel(parentLoggerName)).isEqualTo(parentLevel);
        assertThat(resource.getLoggerLevel(loggerName)).isEqualTo(expectedLevel);

        resource.setLoggerLevel(loggerName, originalLevel);
        assertThat(resource.getLoggerLevel(parentLoggerName)).isEqualTo(parentLevel);
        assertThat(resource.getLoggerLevel(loggerName)).isEqualTo(originalLevel);
    }
}
