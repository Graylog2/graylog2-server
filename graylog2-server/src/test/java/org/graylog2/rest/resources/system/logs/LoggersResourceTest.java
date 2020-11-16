/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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
