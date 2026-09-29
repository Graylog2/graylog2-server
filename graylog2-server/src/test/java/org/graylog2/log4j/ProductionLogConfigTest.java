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
package org.graylog2.log4j;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests logger configurations in the production {@code log4j2.xml}. The config is loaded into a private
 * {@link LoggerContext} because the global test context uses {@code log4j2-test.xml}.
 */
public class ProductionLogConfigTest {
    private static final String ABFS_DATE_LOGGER = "org.apache.hadoop.fs.azurebfs.utils.DateTimeUtils";

    @Test
    void abfsNullDateSpamIsSuppressedButOtherParseFailuresAreLogged() throws Exception {
        try (final InputStream configStream = getClass().getResourceAsStream("/log4j2.xml")) {
            final LoggerContext context = new LoggerContext("production-log4j2-xml");
            context.start(new XmlConfiguration(context, new ConfigurationSource(configStream)));
            try {
                final LoggerConfig loggerConfig = context.getConfiguration().getLoggerConfig(ABFS_DATE_LOGGER);
                assertThat(loggerConfig.getName()).isEqualTo(ABFS_DATE_LOGGER);

                final List<String> messages = new CopyOnWriteArrayList<>();
                final AbstractAppender capture =
                        new AbstractAppender("capture", null, null, true, Property.EMPTY_ARRAY) {
                            @Override
                            public void append(final LogEvent event) {
                                messages.add(event.getMessage().getFormattedMessage());
                            }
                        };
                capture.start();
                loggerConfig.addAppender(capture, Level.ALL, null);

                final Logger logger = context.getLogger(ABFS_DATE_LOGGER);
                // hadoop-azure FNS blob listings log this for virtual directories, which have no Last-Modified
                // value. See the comment on the logger entry in log4j2.xml.
                logger.error("Failed to parse the date {}", "null");
                // A genuinely malformed date must remain visible.
                logger.error("Failed to parse the date {}", "Tue, 99 Foo 2026 25:61:61 GMT");

                assertThat(messages).containsExactly("Failed to parse the date Tue, 99 Foo 2026 25:61:61 GMT");
            } finally {
                context.stop();
            }
        }
    }
}
