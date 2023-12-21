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

import org.graylog.datanode.management.OpensearchProcess;

import jakarta.inject.Inject;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/logs")
@Produces(MediaType.APPLICATION_JSON)
public class LogsController {
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
}
