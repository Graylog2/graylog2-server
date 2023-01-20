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

import org.graylog.datanode.management.ManagedNodes;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/logs")
@Produces(MediaType.APPLICATION_JSON)
public class LogsController {
    private final ManagedNodes managedOpensearch;

    @Inject
    public LogsController(ManagedNodes managedOpenSearch) {
        this.managedOpensearch = managedOpenSearch;
    }

    @GET
    @Path("/{processId}/stdout")
    public List<String> getOpensearchStdout(@PathParam("processId") int processId) {
        return managedOpensearch.getProcesses()
                .stream()
                .filter(p -> p.hasPid(processId))
                .findFirst()
                .map(node -> node.getProcessLogs().getStdOut())
                .orElseThrow(() -> new IllegalArgumentException("Process not found: " + processId));
    }

    @GET
    @Path("/{processId}/stderr")
    public List<String> getOpensearchStderr(@PathParam("processId")  int processId) {
        return managedOpensearch.getProcesses()
                .stream()
                .filter(p -> p.hasPid(processId))
                .findFirst()
                .map(node -> node.getProcessLogs().getStdErr())
                .orElseThrow(() -> new IllegalArgumentException("Process not found: " + processId));
    }
}
