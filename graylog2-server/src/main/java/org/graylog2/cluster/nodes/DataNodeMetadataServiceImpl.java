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
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DataNodeMetadataServiceImpl implements DataNodeMetadataService {

    private final MongoCollection<DataNodeMetadata> collection;

    @Inject
    public DataNodeMetadataServiceImpl(MongoCollections mongoCollections) {
        this.collection = mongoCollections.collection(DataNodeMetadata.COLLECTION_NAME, DataNodeMetadata.class);
    }

    @Override
    public void setOpensearchVersions(String nodeId, String currentVersion, @Nullable String latestAvailableVersion) {
        final List<Bson> updates = new ArrayList<>();
        updates.add(Updates.setOnInsert(DataNodeMetadata.FIELD_NODE_ID, nodeId));
        updates.add(Updates.set(DataNodeMetadata.FIELD_CURRENT_OPENSEARCH_VERSION, currentVersion));
        if (latestAvailableVersion != null) {
            updates.add(Updates.set(DataNodeMetadata.FIELD_LATEST_AVAILABLE_OPENSEARCH_VERSION, latestAvailableVersion));
        } else {
            updates.add(Updates.unset(DataNodeMetadata.FIELD_LATEST_AVAILABLE_OPENSEARCH_VERSION));
        }
        collection.updateOne(
                Filters.eq(DataNodeMetadata.FIELD_NODE_ID, nodeId),
                Updates.combine(updates),
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
        return OpensearchVersionsOverview.of(nodes);
    }
}
