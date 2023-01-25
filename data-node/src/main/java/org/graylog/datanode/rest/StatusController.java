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
import org.graylog2.plugin.Version;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class StatusController {

    private final Version version = Version.CURRENT_CLASSPATH;

    private final OpensearchProcess openSearch;

    @Inject
    public StatusController(OpensearchProcess openSearch) {
        this.openSearch = openSearch;
    }

    @GET
    public DataNodeStatus status() {
        return new DataNodeStatus(
                version,
                new StatusResponse(openSearch.opensearchVersion(), openSearch.processInfo())
        );
    }
}
