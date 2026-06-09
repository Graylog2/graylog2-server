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
package org.graylog2.cluster.nodes;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.datanode.DataNodeInformation;
import org.graylog2.datanode.DatanodeUpgradeService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataNodeMetadataServiceImpl implements DataNodeMetadataService {

    private final MongoCollection<DataNodeMetadata> collection;
    private final DataNodeClusterService dataNodeClusterService;
    private final DatanodeUpgradeService datanodeUpgradeService;

    @Inject
    public DataNodeMetadataServiceImpl(MongoCollections mongoCollections,
                                       DataNodeClusterService dataNodeClusterService,
                                       DatanodeUpgradeService datanodeUpgradeService) {
        this.collection = mongoCollections.collection(DataNodeMetadata.COLLECTION_NAME, DataNodeMetadata.class);
        this.dataNodeClusterService = dataNodeClusterService;
        this.datanodeUpgradeService = datanodeUpgradeService;
    }

    @Override
    public void setOpensearchVersion(String nodeId, String version) {
        collection.updateOne(
                Filters.eq(DataNodeMetadata.FIELD_NODE_ID, nodeId),
                Updates.combine(
                        Updates.setOnInsert(DataNodeMetadata.FIELD_NODE_ID, nodeId),
                        Updates.set(DataNodeMetadata.FIELD_CURRENT_OPENSEARCH_VERSION, version)
                ),
                new UpdateOptions().upsert(true)
        );
    }

    @Override
    public void setLatestAvailableOpensearchVersion(String nodeId, String version) {
        collection.updateOne(
                Filters.eq(DataNodeMetadata.FIELD_NODE_ID, nodeId),
                Updates.combine(
                        Updates.setOnInsert(DataNodeMetadata.FIELD_NODE_ID, nodeId),
                        Updates.set(DataNodeMetadata.FIELD_LATEST_AVAILABLE_OPENSEARCH_VERSION, version)
                ),
                new UpdateOptions().upsert(true)
        );
    }

    @Override
    public Optional<DataNodeMetadata> findByNodeId(String nodeId) {
        return Optional.ofNullable(
                collection.find(Filters.eq(DataNodeMetadata.FIELD_NODE_ID, nodeId)).first()
        );
    }

    @Override
    public OpensearchVersionsOverview getVersionsOverview() {
        final List<DataNodeMetadata> nodes = collection.find().into(new ArrayList<>());
        final Set<String> nodeIds = nodes.stream().map(DataNodeMetadata::nodeId).collect(Collectors.toSet());
        final Map<String, DataNodeDto> datanodeDtos = dataNodeClusterService.byNodeIds(nodeIds);

        final var upgradeStatus = datanodeUpgradeService.status();
        final Map<String, DataNodeInformation> informationByHostname = Stream
                .concat(upgradeStatus.upToDateNodes().stream(), upgradeStatus.outdatedNodes().stream())
                .collect(Collectors.toMap(DataNodeInformation::hostname, i -> i));

        final Map<String, DataNodeInformation> informationByNodeId = datanodeDtos.entrySet().stream()
                .filter(e -> e.getValue().getHostname() != null)
                .filter(e -> informationByHostname.containsKey(e.getValue().getHostname()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> informationByHostname.get(e.getValue().getHostname())
                ));

        return OpensearchVersionsOverview.of(nodes, informationByNodeId);
    }
}
