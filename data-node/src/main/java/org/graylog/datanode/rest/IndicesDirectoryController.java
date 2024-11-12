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

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.DirectoryReadableValidator;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.filesystem.index.IndicesDirectoryParser;
import org.graylog.datanode.filesystem.index.dto.IndexerDirectoryInformation;
import org.graylog.datanode.filesystem.index.dto.NodeInformation;
import org.graylog.shaded.opensearch2.org.opensearch.Version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Path("/indices-directory")
@Produces(MediaType.APPLICATION_JSON)
public class IndicesDirectoryController {

    private final Configuration configuration;
    private final DatanodeConfiguration datanodeConfiguration;
    private final IndicesDirectoryParser indicesDirectoryParser;
    private final DirectoryReadableValidator directoryReadableValidator = new DirectoryReadableValidator();

    @Inject
    public IndicesDirectoryController(Configuration configuration, DatanodeConfiguration datanodeConfiguration, IndicesDirectoryParser indicesDirectoryParser) {
        this.configuration = configuration;
        this.datanodeConfiguration = datanodeConfiguration;
        this.indicesDirectoryParser = indicesDirectoryParser;
    }

    @GET
    @Path("compatibility")
    public CompatibilityResult status() {
        final java.nio.file.Path dataTargetDir = datanodeConfiguration.datanodeDirectories().getDataTargetDir();
        final String opensearchVersion = datanodeConfiguration.opensearchDistributionProvider().get().version();
        final String hostname = configuration.getHostname();
        try {
            directoryReadableValidator.validate(dataTargetDir.toUri().toString(), dataTargetDir);
            final IndexerDirectoryInformation info = indicesDirectoryParser.parse(dataTargetDir);
            final Version currentVersion = Version.fromString(opensearchVersion);

            final List<String> compatibilityWarnings = new ArrayList<>();

            if (info.nodes().isEmpty() || info.nodes().stream().allMatch(n -> n.indices().isEmpty())) {
                compatibilityWarnings.add("Your configured opensearch_data_location directory " + dataTargetDir.toAbsolutePath() + " doesn't contain any indices! Do you want to continue without migrating existing data?");
            }

            final List<String> compatibilityErrors = info.nodes().stream()
                    .filter(node -> !isNodeCompatible(node, currentVersion))
                    .map(node -> String.format(Locale.ROOT, "Current version %s of Opensearch is not compatible with index version %s", currentVersion, node.nodeVersion()))
                    .toList();

            return new CompatibilityResult(hostname, opensearchVersion, info, compatibilityErrors, compatibilityWarnings);
        } catch (Exception e) {
            return new CompatibilityResult(hostname, opensearchVersion, new IndexerDirectoryInformation(dataTargetDir, Collections.emptyList()), Collections.singletonList(e.getMessage()), Collections.emptyList());
        }
    }

    private static boolean isNodeCompatible(NodeInformation node, Version currentVersion) {
        final Version nodeVersion = Version.fromString(node.nodeVersion());
        return node.nodeVersion() == null || currentVersion.isCompatible(nodeVersion);
    }
}
