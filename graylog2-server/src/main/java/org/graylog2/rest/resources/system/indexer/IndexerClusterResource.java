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
package org.graylog2.rest.resources.system.indexer;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.rest.models.system.indexer.responses.ClusterHealth;
import org.graylog2.rest.models.system.indexer.responses.ClusterInfo;
import org.graylog2.rest.models.system.indexer.responses.ClusterName;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.storage.providers.ElasticsearchVersionProvider;

@RequiresAuthentication
@Api(value = "Indexer/Cluster", description = "Indexer cluster information")
@Path("/system/indexer/cluster")
public class IndexerClusterResource extends RestResource {

    private final Cluster cluster;
    private final ElasticsearchVersionProvider elasticsearchVersionProvider;

    @Inject
    public IndexerClusterResource(Cluster cluster, ElasticsearchVersionProvider elasticsearchVersionProvider) {
        this.cluster = cluster;
        this.elasticsearchVersionProvider = elasticsearchVersionProvider;
    }

    @GET
    @Timed
    @Path("/name")
    @RequiresPermissions(RestPermissions.INDEXERCLUSTER_READ)
    @ApiOperation(value = "Get the cluster name")
    @Produces(MediaType.APPLICATION_JSON)
    public ClusterName clusterName() {
        final String clusterName = cluster.clusterName()
                .orElseThrow(() -> new InternalServerErrorException("Couldn't read Elasticsearch cluster health"));
        return ClusterName.create(clusterName);
    }

    @GET
    @Timed
    @Path("/info")
    @RequiresPermissions(RestPermissions.INDEXERCLUSTER_READ)
    @ApiOperation(value = "Get cluster name and distribution")
    @Produces(MediaType.APPLICATION_JSON)
    public ClusterInfo clusterInfo() {
        final String clusterName = cluster.clusterName()
                .orElseThrow(() -> new InternalServerErrorException("Couldn't read indexer cluster info"));
        return new ClusterInfo(clusterName, elasticsearchVersionProvider.get().distribution().toString());
    }

    @GET
    @Timed
    @Path("/health")
    @ApiOperation(value = "Get cluster and shard health overview")
    @RequiresPermissions(RestPermissions.INDEXERCLUSTER_READ)
    @Produces(MediaType.APPLICATION_JSON)
    public ClusterHealth clusterHealth() {
        return cluster.clusterHealthStats()
                .orElseThrow(() -> new InternalServerErrorException("Couldn't read Elasticsearch cluster health"));
    }
}
