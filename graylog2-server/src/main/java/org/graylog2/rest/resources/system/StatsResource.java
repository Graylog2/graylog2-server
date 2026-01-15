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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.system.stats.StatsService;
import org.graylog2.shared.system.stats.SystemStats;
import org.graylog2.shared.system.stats.fs.FsStats;
import org.graylog2.shared.system.stats.jvm.JvmStats;
import org.graylog2.shared.system.stats.network.NetworkStats;
import org.graylog2.shared.system.stats.os.OsStats;
import org.graylog2.shared.system.stats.process.ProcessStats;

import jakarta.inject.Inject;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Tag(name = "System/Stats", description = "Node system stats")
@Path("/system/stats")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class StatsResource extends RestResource {
    private final StatsService statsService;

    @Inject
    public StatsResource(StatsService statsService) {
        this.statsService = statsService;
    }

    @GET
    @Timed
    @Operation(summary = "System information about this node.",
                  description = "This resource returns information about the system this node is running on.")
    public SystemStats systemStats() {
        return statsService.systemStatsWithoutNetwork();
    }

    @GET
    @Path("/fs")
    @Timed
    @Operation(summary = "Filesystem information about this node.",
                  description = "This resource returns information about the filesystems of this node.")
    public FsStats fsStats() {
        return statsService.fsStats();
    }

    @GET
    @Path("/jvm")
    @Timed
    @Operation(summary = "JVM information about this node.",
                  description = "This resource returns information about the Java Virtual Machine of this node.")
    public JvmStats jvmStats() {
        return statsService.jvmStats();
    }

    @GET
    @Path("/network")
    @Timed
    @Operation(summary = "Networking information about this node.",
                  description = "This resource returns information about the networking system this node is running with.")
    public NetworkStats networkStats() {
        return statsService.networkStats();
    }

    @GET
    @Path("/os")
    @Timed
    @Operation(summary = "OS information about this node.",
                  description = "This resource returns information about the operating system this node is running on.")
    public OsStats osStats() {
        return statsService.osStats();
    }

    @GET
    @Path("/process")
    @Timed
    @Operation(summary = "Process information about this node.",
                  description = "This resource returns information about the process this node is running as.")
    public ProcessStats processStats() {
        return statsService.processStats();
    }
}
