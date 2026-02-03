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
package org.graylog.datanode.rest;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.graylog.datanode.opensearch.OpensearchProcess;

import jakarta.inject.Inject;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.graylog.datanode.rest.config.OnlyInSecuredNode;
import org.graylog2.log4j.MemoryAppender;

import java.util.List;

@Path("/logs")
@Produces(MediaType.APPLICATION_JSON)
public class LogsController {

    private static final String MEMORY_APPENDER_NAME = "datanode-internal-logs";

    private final OpensearchProcess managedOpensearch;

    @Inject
    public LogsController(OpensearchProcess managedOpenSearch) {
        this.managedOpensearch = managedOpenSearch;
    }

    @GET
    @Path("/stdout")
    public List<String> getOpensearchStdout() {
        return managedOpensearch.stdOutLogs();
    }

    @GET
    @Path("/stderr")
    public List<String> getOpensearchStderr() {
        return managedOpensearch.stdErrLogs();
    }

    @GET
    @OnlyInSecuredNode
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/internal")
    public Response getOpensearchInternal() {
        final Appender appender = getAppender(MEMORY_APPENDER_NAME);
        if (appender == null) {
            throw new NotFoundException("Memory appender is disabled. Please refer to the example log4j.xml file.");
        }

        if (!(appender instanceof MemoryAppender memoryAppender)) {
            throw new InternalServerErrorException("Memory appender is not an instance of MemoryAppender. Please refer to the example log4j.xml file.");
        }
        var mediaType = MediaType.valueOf(MediaType.TEXT_PLAIN);

        StreamingOutput streamingOutput = outputStream -> memoryAppender.streamFormattedLogMessages(outputStream, 0);
        Response.ResponseBuilder response = Response.ok(streamingOutput, mediaType);

        return response.build();
    }


    private Appender getAppender(final String appenderName) {
        final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        final Configuration configuration = loggerContext.getConfiguration();
        return configuration.getAppender(appenderName);
    }
}
