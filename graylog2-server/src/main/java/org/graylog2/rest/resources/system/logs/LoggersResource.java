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

import com.codahale.metrics.annotation.Timed;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.log4j.MemoryAppender;
import org.graylog2.rest.models.system.loggers.responses.InternalLogMessage;
import org.graylog2.rest.models.system.loggers.responses.LogMessagesSummary;
import org.graylog2.rest.models.system.loggers.responses.LoggersSummary;
import org.graylog2.rest.models.system.loggers.responses.SingleLoggerSummary;
import org.graylog2.rest.models.system.loggers.responses.SingleSubsystemSummary;
import org.graylog2.rest.models.system.loggers.responses.SubsystemSummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RequiresAuthentication
@Api(value = "System/Loggers", description = "Internal Graylog loggers")
@Path("/system/loggers")
public class LoggersResource extends RestResource {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(LoggersResource.class);
    private static final String MEMORY_APPENDER_NAME = "graylog-internal-logs";

    private static final Map<String, Subsystem> SUBSYSTEMS = ImmutableMap.<String, Subsystem>of(
            "graylog", new Subsystem("Graylog", ImmutableList.of("org.graylog2", "org.graylog"), "All messages from Graylog-owned systems."),
            "indexer", new Subsystem("Indexer", "org.elasticsearch", "All messages related to indexing and searching."),
            "authentication", new Subsystem("Authentication", "org.apache.shiro", "All user authentication messages."),
            "sockets", new Subsystem("Sockets", "netty", "All messages related to socket communication."));

    @GET
    @Timed
    @ApiOperation(value = "List all loggers and their current levels")
    @Produces(MediaType.APPLICATION_JSON)
    public LoggersSummary loggers() {
        final Collection<LoggerConfig> loggerConfigs = getLoggerConfigs();
        final Map<String, SingleLoggerSummary> loggers = Maps.newHashMapWithExpectedSize(loggerConfigs.size());
        for (LoggerConfig config : loggerConfigs) {
            if (!isPermitted(RestPermissions.LOGGERS_READ, config.getName())) {
                continue;
            }

            final Level level = config.getLevel();
            loggers.put(config.getName(), SingleLoggerSummary.create(level.toString().toLowerCase(Locale.ENGLISH), level.intLevel()));
        }

        return LoggersSummary.create(loggers);
    }

    @VisibleForTesting
    protected Collection<LoggerConfig> getLoggerConfigs() {
        final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        final Configuration configuration = loggerContext.getConfiguration();
        return configuration.getLoggers().values();
    }

    @GET
    @Timed
    @Path("/subsystems")
    @ApiOperation(value = "List all logger subsystems and their current levels")
    @Produces(MediaType.APPLICATION_JSON)
    public SubsystemSummary subsystems() {
        final Map<String, SingleSubsystemSummary> subsystems = Maps.newHashMap();
        for (Map.Entry<String, Subsystem> subsystem : SUBSYSTEMS.entrySet()) {
            if (!isPermitted(RestPermissions.LOGGERS_READSUBSYSTEM, subsystem.getKey())) {
                continue;
            }

            try {
                final String category = subsystem.getValue().getCategories().get(0);
                final Level level = getLoggerLevel(category);

                subsystems.put(subsystem.getKey(),
                        SingleSubsystemSummary.create(
                                subsystem.getValue().getTitle(),
                                subsystem.getValue().getCategories(),
                                subsystem.getValue().getDescription(),
                                level.toString().toLowerCase(Locale.ENGLISH),
                                level.intLevel()));
            } catch (Exception e) {
                LOG.error("Error while listing logger subsystem.", e);
            }
        }

        return SubsystemSummary.create(subsystems);
    }

    @VisibleForTesting
    protected Level getLoggerLevel(final String loggerName) {
        final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        final Configuration configuration = loggerContext.getConfiguration();
        final LoggerConfig loggerConfig = configuration.getLoggerConfig(loggerName);

        return loggerConfig.getLevel();
    }

    @VisibleForTesting
    protected void setLoggerLevel(final String loggerName, final Level level) {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final Configuration config = context.getConfiguration();
        final LoggerConfig loggerConfig = config.getLoggerConfig(loggerName);
        if(loggerName.equals(loggerConfig.getName())) {
            loggerConfig.setLevel(level);
        } else {
            final LoggerConfig newLoggerConfig = new LoggerConfig(loggerName, level, loggerConfig.isAdditive());
            newLoggerConfig.setLevel(level);
            config.addLogger(loggerName, newLoggerConfig);
        }
        context.updateLoggers();
    }

    @PUT
    @Timed
    @ApiOperation(value = "Set the loglevel of a whole subsystem",
            notes = "Provided level is falling back to DEBUG if it does not exist")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such subsystem.")
    })
    @Path("/subsystems/{subsystem}/level/{level}")
    @AuditEvent(type = AuditEventTypes.LOG_LEVEL_UPDATE)
    public void setSubsystemLoggerLevel(
        @ApiParam(name = "subsystem", required = true) @PathParam("subsystem") @NotEmpty String subsystemTitle,
        @ApiParam(name = "level", required = true) @PathParam("level") @NotEmpty String level) {
        if (!SUBSYSTEMS.containsKey(subsystemTitle)) {
            final String msg = "No such logging subsystem: [" + subsystemTitle + "]";
            LOG.warn(msg);
            throw new NotFoundException(msg);
        }
        checkPermission(RestPermissions.LOGGERS_EDITSUBSYSTEM, subsystemTitle);

        final Subsystem subsystem = SUBSYSTEMS.get(subsystemTitle);
        final Level newLevel = Level.toLevel(level.toUpperCase(Locale.ENGLISH));
        for (String category: subsystem.getCategories()) {
            setLoggerLevel(category, newLevel);
        }

        LOG.debug("Successfully set log level for subsystem \"{}\" to \"{}\"", subsystem.getTitle(), newLevel);
    }

    @PUT
    @Timed
    @ApiOperation(value = "Set the loglevel of a single logger",
            notes = "Provided level is falling back to DEBUG if it does not exist")
    @Path("/{loggerName}/level/{level}")
    @AuditEvent(type = AuditEventTypes.LOG_LEVEL_UPDATE)
    public void setSingleLoggerLevel(
        @ApiParam(name = "loggerName", required = true) @PathParam("loggerName") @NotEmpty String loggerName,
        @ApiParam(name = "level", required = true) @NotEmpty @PathParam("level") String level) {
        checkPermission(RestPermissions.LOGGERS_EDIT, loggerName);
        final Level newLevel = Level.toLevel(level.toUpperCase(Locale.ENGLISH));
        setLoggerLevel(loggerName, newLevel);

        LOG.debug("Successfully set log level for logger \"{}\" to \"{}\"", loggerName, newLevel);
    }

    @GET
    @Timed
    @ApiOperation(value = "Get recent internal log messages")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Memory appender is disabled."),
            @ApiResponse(code = 500, message = "Memory appender is broken.")
    })
    @Path("/messages/recent")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(RestPermissions.LOGGERSMESSAGES_READ)
    public LogMessagesSummary messages(@ApiParam(name = "limit", value = "How many log messages should be returned", defaultValue = "500", allowableValues = "range[0, infinity]")
                                       @QueryParam("limit") @DefaultValue("500") @Min(0L) int limit,
                                       @ApiParam(name = "level", value = "Which log level (or higher) should the messages have", defaultValue = "ALL", allowableValues = "[OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL]")
                                       @QueryParam("level") @DefaultValue("ALL") @NotEmpty String level) {
        final Appender appender = getAppender(MEMORY_APPENDER_NAME);
        if (appender == null) {
            throw new NotFoundException("Memory appender is disabled. Please refer to the example log4j.xml file.");
        }

        if (!(appender instanceof MemoryAppender)) {
            throw new InternalServerErrorException("Memory appender is not an instance of MemoryAppender. Please refer to the example log4j.xml file.");
        }

        final Level logLevel = Level.toLevel(level, Level.ALL);
        final MemoryAppender memoryAppender = (MemoryAppender) appender;
        final List<InternalLogMessage> messages = new ArrayList<>(limit);
        for (LogEvent event : memoryAppender.getLogMessages(limit)) {
            final Level eventLevel = event.getLevel();
            if (!eventLevel.isMoreSpecificThan(logLevel)) {
                continue;
            }

            final ThrowableProxy thrownProxy = event.getThrownProxy();
            final String throwable;
            if (thrownProxy == null) {
                throwable = null;
            } else {
                throwable = thrownProxy.getExtendedStackTraceAsString("");
            }

            final Marker marker = event.getMarker();
            messages.add(InternalLogMessage.create(
                    event.getMessage().getFormattedMessage(),
                    event.getLoggerName(),
                    eventLevel.toString(),
                    marker == null ? null : marker.toString(),
                    new DateTime(event.getTimeMillis(), DateTimeZone.UTC),
                    throwable,
                    event.getThreadName(),
                    event.getContextData().toMap()
            ));
        }

        return LogMessagesSummary.create(messages);
    }

    private Appender getAppender(final String appenderName) {
        final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        final Configuration configuration = loggerContext.getConfiguration();
        return configuration.getAppender(appenderName);
    }

    private static class Subsystem {
        private final String title;
        private final List<String> categories;
        private final String description;

        public Subsystem(String title, String category, String description) {
            this.title = title;
            this.categories = ImmutableList.of(category);
            this.description = description;
        }

        public Subsystem(String title, List<String> categories, String description) {
            this.title = title;
            this.categories = ImmutableList.copyOf(categories);
            this.description = description;
        }

        private String getTitle() {
            return title;
        }

        private List<String> getCategories() {
            return categories;
        }

        private String getDescription() {
            return description;
        }
    }
}
