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
import org.graylog2.plugin.Version;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.stream.Collectors;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class StatusController {

    private final Version version = Version.CURRENT_CLASSPATH;

    private ManagedNodes openSearch;

    @Inject
    public StatusController(ManagedNodes openSearch) {
        this.openSearch = openSearch;
    }

    @GET
    public DataNodeStatus status() {

        return openSearch.getProcesses()
                .stream()
                .map(process -> new StatusResponse(process.getOpensearchVersion(), process.getProcessInfo()))
                .collect(Collectors.collectingAndThen(Collectors.toList(), statusResponses -> new DataNodeStatus(version, statusResponses)));
    }
}
