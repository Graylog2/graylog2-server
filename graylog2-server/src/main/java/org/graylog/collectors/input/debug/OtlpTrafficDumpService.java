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
package org.graylog.collectors.input.debug;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.protobuf.util.JsonFormat;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.graylog.collectors.CollectorJournal;
import org.graylog2.configuration.PathConfiguration;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.logging.log4j.Level.INFO;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * {@link OtlpTrafficDump} backed by a Log4j 2 {@link RollingFileAppender} that writes
 * {@link CollectorJournal.Record} protobuf messages as NDJSON. The appender is created on
 * {@link #startUp()} and torn down on {@link #shutDown()}.
 */
@Singleton
public class OtlpTrafficDumpService extends AbstractIdleService implements OtlpTrafficDump {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(OtlpTrafficDumpService.class);

    private static final String APPENDER_NAME = "otlp-traffic-dump";
    private static final String LOGGER_NAME = "otlp-traffic-dump";
    private static final String DUMP_DIR_NAME = "collector-otlp-dump";
    private static final String FILE_NAME = "collector-otlp-dump.ndjson";
    private static final String FILE_PATTERN = "collector-otlp-dump-%i.ndjson.gz";
    private static final String MAX_FILE_SIZE = "1GB";
    private static final int MAX_ROLLOVER_FILES = 20;

    private final JsonFormat.Printer jsonPrinter;
    private final Path dumpDir;
    private final LoggerContext loggerContext;

    private Appender appender;
    private Logger dumpLogger;

    @Inject
    public OtlpTrafficDumpService(PathConfiguration pathConfiguration) {
        this(pathConfiguration.getDataDir().resolve(DUMP_DIR_NAME));
    }

    @VisibleForTesting
    OtlpTrafficDumpService(Path dumpDir) {
        this.jsonPrinter = JsonFormat.printer().omittingInsignificantWhitespace();
        this.dumpDir = dumpDir;
        this.loggerContext = (LoggerContext) LogManager.getContext(false);
    }

    @Override
    public void write(CollectorJournal.Record record) {
        try {
            dumpLogger.info(jsonPrinter.print(record));
        } catch (Exception e) {
            LOG.warn("Failed to write OTLP traffic dump", e);
        }
    }

    @Override
    protected void startUp() throws Exception {
        this.appender = createAppender(dumpDir, loggerContext);
        this.dumpLogger = createDumpLogger(loggerContext, appender);
        LOG.info("OTLP traffic dump enabled, writing to {}", dumpDir);
    }

    @Override
    protected void shutDown() throws Exception {
        appender.stop();
        final var config = loggerContext.getConfiguration();
        config.removeLogger(LOGGER_NAME);
        loggerContext.updateLoggers();
        LOG.info("OTLP traffic dump stopped");
    }

    private static Appender createAppender(Path dumpDir, LoggerContext ctx) {
        try {
            Files.createDirectories(dumpDir);
        } catch (IOException e) {
            throw new RuntimeException(f("Failed to create OTLP dump directory: %s", dumpDir), e);
        }

        final var config = ctx.getConfiguration();

        final var layout = PatternLayout.newBuilder()
                .withPattern("%m%n")
                .withConfiguration(config)
                .build();

        final var policy = SizeBasedTriggeringPolicy.createPolicy(MAX_FILE_SIZE);

        final var strategy = DefaultRolloverStrategy.newBuilder()
                .withMax(String.valueOf(MAX_ROLLOVER_FILES))
                .withConfig(config)
                .build();

        final var appender = RollingFileAppender.newBuilder()
                .setConfiguration(config)
                .setName(APPENDER_NAME)
                .setLayout(layout)
                .withFileName(dumpDir.resolve(FILE_NAME).toString())
                .withFilePattern(dumpDir.resolve(FILE_PATTERN).toString())
                .withPolicy(policy)
                .withStrategy(strategy)
                .build();
        appender.start();

        return appender;
    }

    private static Logger createDumpLogger(LoggerContext ctx, Appender appender) {
        final var config = ctx.getConfiguration();

        final var loggerConfig = new LoggerConfig(LOGGER_NAME, INFO, false);
        loggerConfig.addAppender(appender, INFO, null);
        config.addLogger(LOGGER_NAME, loggerConfig);
        ctx.updateLoggers();

        return ctx.getLogger(LOGGER_NAME);
    }
}
