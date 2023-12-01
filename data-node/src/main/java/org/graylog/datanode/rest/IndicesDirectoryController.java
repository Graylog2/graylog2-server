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
import org.graylog.datanode.filesystem.index.IncompatibleIndexVersionException;
import org.graylog.datanode.filesystem.index.IndicesDirectoryParser;
import org.graylog.datanode.filesystem.index.dto.IndexerDirectoryInformation;
import org.graylog.datanode.filesystem.index.dto.NodeInformation;
import org.graylog.datanode.rest.config.OnlyInSecuredNode;
import org.graylog.shaded.opensearch2.org.opensearch.Version;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Path("/indices-directory")
@Produces(MediaType.APPLICATION_JSON)
public class IndicesDirectoryController {

    private final DatanodeConfiguration datanodeConfiguration;
    private final IndicesDirectoryParser indicesDirectoryParser;

    @Inject
    public IndicesDirectoryController(DatanodeConfiguration datanodeConfiguration, IndicesDirectoryParser indicesDirectoryParser) {
        this.datanodeConfiguration = datanodeConfiguration;
        this.indicesDirectoryParser = indicesDirectoryParser;
    }

    @OnlyInSecuredNode
    @GET
    @Path("compatibility")
    public CompatibilityResult status() {
        final java.nio.file.Path dataTargetDir = datanodeConfiguration.datanodeDirectories().getDataTargetDir();
        final String opensearchVersion = datanodeConfiguration.opensearchDistributionProvider().get().version();
        try {
            final IndexerDirectoryInformation info = indicesDirectoryParser.parse(dataTargetDir);
            final Version currentVersion = Version.fromString(opensearchVersion);
            final List<String> compatibilityErrors = info.nodes().stream()
                    .filter(node -> !isNodeCompatible(node, currentVersion))
                    .map(node -> String.format(Locale.ROOT, "Current version %s of Opensearch is not compatible with index version %s", currentVersion, node.nodeVersion()))
                    .toList();
            return new CompatibilityResult(opensearchVersion, info, compatibilityErrors);
        } catch (IncompatibleIndexVersionException e) {
            return new CompatibilityResult(opensearchVersion, new IndexerDirectoryInformation(dataTargetDir, Collections.emptyList()), Collections.singletonList(e.getMessage()));
        }
    }

    private static boolean isNodeCompatible(NodeInformation node, Version currentVersion) {
        final Version nodeVersion = Version.fromString(node.nodeVersion());
        return node.nodeVersion() == null || currentVersion.isCompatible(nodeVersion);
    }
}
