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
package org.graylog2.cluster.preflight;

import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.indices.MongoDbIndexTools;
import org.graylog2.shared.utilities.StringUtils;
import org.mongojack.DBQuery;
import org.mongojack.DBUpdate;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static org.graylog2.cluster.preflight.NodePreflightConfig.FIELD_CERTIFICATE;
import static org.graylog2.cluster.preflight.NodePreflightConfig.FIELD_CSR;
import static org.graylog2.cluster.preflight.NodePreflightConfig.FIELD_NODEID;
import static org.graylog2.cluster.preflight.NodePreflightConfig.FIELD_STATE;
import static org.graylog2.cluster.preflight.NodePreflightConfig.State.CSR;
import static org.graylog2.cluster.preflight.NodePreflightConfig.State.SIGNED;

public class NodePreflightConfigServiceImpl extends PaginatedDbService<NodePreflightConfig> implements NodePreflightConfigService {
    public static final String COLLECTION_NAME = "node_preflight_config";

    private final JacksonDBCollection<NodePreflightConfig, String> dbCollection;

    @Inject
    public NodePreflightConfigServiceImpl(final MongoJackObjectMapperProvider mongoJackObjectMapperProvider,
                                          final MongoConnection mongoConnection) {
        super(mongoConnection, mongoJackObjectMapperProvider, NodePreflightConfig.class, COLLECTION_NAME);
        this.dbCollection = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(COLLECTION_NAME), NodePreflightConfig.class, String.class, mongoJackObjectMapperProvider.get());
        new MongoDbIndexTools(db).createUniqueIndex(FIELD_NODEID);
    }

    @Override
    public NodePreflightConfig getPreflightConfigFor(String nodeId) {
        return dbCollection.findOne(DBQuery.is(FIELD_NODEID, nodeId));
    }

    @Override
    public List<NodePreflightConfig> findAllNodesThatNeedAttention() {
        return this.streamQuery(DBQuery.notIn(FIELD_STATE, NodePreflightConfig.State.CONNECTED)).toList();
    }

    @Override
    public void writeCsr(final String nodeId,
                         final String csr) {
        final WriteResult<NodePreflightConfig, String> result = dbCollection.update(
                DBQuery.is(FIELD_NODEID, nodeId),
                new DBUpdate.Builder()
                        .set(FIELD_CSR, csr)
                        .set(FIELD_STATE, CSR),
                false,
                false);

        if (result.getN() != 1) {
            throw new RuntimeException(StringUtils.f("Failed to write node %s certificate", nodeId));
        }
    }

    @Override
    public void writeCert(final String nodeId,
                          final String cert) {
        final WriteResult<NodePreflightConfig, String> result = dbCollection.update(
                DBQuery.is(FIELD_NODEID, nodeId),
                new DBUpdate.Builder()
                        .set(FIELD_CERTIFICATE, cert)
                        .set(FIELD_STATE, SIGNED),
                false,
                false);

        if (result.getN() != 1) {
            throw new RuntimeException(StringUtils.f("Failed to write node %s certificate", nodeId));
        }
    }

    @Override
    public Optional<String> readCert(final String nodeId) {
        final NodePreflightConfig config = dbCollection.findOne(
                DBQuery.is(FIELD_NODEID, nodeId)
        );
        if (config != null) {
            return Optional.ofNullable(config.certificate());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void changeState(final String nodeId, final NodePreflightConfig.State state) {
        final WriteResult<NodePreflightConfig, String> result = dbCollection.update(
                DBQuery.is(FIELD_NODEID, nodeId),
                new DBUpdate.Builder()
                        .set(FIELD_STATE, state),
                false,
                false);

        if (result.getN() != 1) {
            throw new RuntimeException(StringUtils.f("Failed to change node %s state", nodeId));
        }

    }

    public void deleteAll() {
        dbCollection.drop();
    }
}
