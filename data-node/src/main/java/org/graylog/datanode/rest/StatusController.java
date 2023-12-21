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

import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.management.OpensearchProcess;
import org.graylog2.plugin.Version;

import jakarta.inject.Inject;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class StatusController {

    private final Version version = Version.CURRENT_CLASSPATH;

    private final DatanodeConfiguration datanodeConfiguration;
    private final OpensearchProcess openSearch;

    @Inject
    public StatusController(DatanodeConfiguration datanodeConfiguration, OpensearchProcess openSearch) {
        this.datanodeConfiguration = datanodeConfiguration;
        this.openSearch = openSearch;
    }

    @GET
    public DataNodeStatus status() {
        return new DataNodeStatus(
                version,
                new StatusResponse(datanodeConfiguration.opensearchDistributionProvider().get().version(), openSearch.processInfo())
        );
    }

}
