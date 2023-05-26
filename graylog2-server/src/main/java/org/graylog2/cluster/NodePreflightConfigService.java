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
package org.graylog2.cluster;

import org.graylog.security.certutil.csr.CsrGenerator;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.indices.MongoDbIndexTools;
import org.mongojack.DBQuery;
import org.mongojack.DBUpdate;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

import static org.graylog2.cluster.NodePreflightConfig.FIELD_CERTIFICATE;
import static org.graylog2.cluster.NodePreflightConfig.FIELD_CSR;
import static org.graylog2.cluster.NodePreflightConfig.FIELD_NODEID;
import static org.graylog2.cluster.NodePreflightConfig.FIELD_STATE;
import static org.graylog2.cluster.NodePreflightConfig.State.CSR;
import static org.graylog2.cluster.NodePreflightConfig.State.SIGNED;

public class NodePreflightConfigService extends PaginatedDbService<NodePreflightConfig> {
    public static final String COLLECTION_NAME = "node_preflight_config";

    private static final Logger LOG = LoggerFactory.getLogger(NodePreflightConfigService.class);
    private final JacksonDBCollection<NodePreflightConfig, String> dbCollection;
    private final CsrGenerator csrGenerator;

    @Inject
    public NodePreflightConfigService(final MongoJackObjectMapperProvider mongoJackObjectMapperProvider,
                                      final MongoConnection mongoConnection,
                                      final CsrGenerator csrGenerator) {
        super(mongoConnection, mongoJackObjectMapperProvider, NodePreflightConfig.class, COLLECTION_NAME);
        this.dbCollection = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(COLLECTION_NAME), NodePreflightConfig.class, String.class, mongoJackObjectMapperProvider.get());
        new MongoDbIndexTools(db).createUniqueIndex(FIELD_NODEID);
        this.csrGenerator = csrGenerator;
    }

    public NodePreflightConfig getPreflightConfigFor(Node node) {
        return getPreflightConfigFor(node.getNodeId());
    }

    public NodePreflightConfig getPreflightConfigFor(String nodeId) {
        return dbCollection.findOne(DBQuery.is(FIELD_NODEID, nodeId));
    }

    public boolean writeCsr(final String nodeId,
                            final String csr) {
        final WriteResult<NodePreflightConfig, String> result = dbCollection.update(
                DBQuery.is(FIELD_NODEID, nodeId),
                new DBUpdate.Builder()
                        .set(FIELD_CSR, csr)
                        .set(FIELD_STATE, CSR),
                false,
                false);

        return result.getN() > 0;
    }

    public boolean writeCert(final String nodeId,
                            final String cert) {
        final WriteResult<NodePreflightConfig, String> result = dbCollection.update(
                DBQuery.is(FIELD_NODEID, nodeId),
                new DBUpdate.Builder()
                        .set(FIELD_CERTIFICATE, cert)
                        .set(FIELD_STATE, SIGNED),
                false,
                false);

        return result.getN() > 0;
    }

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

    public boolean changeState(final String nodeId, final NodePreflightConfig.State state) {
        final WriteResult<NodePreflightConfig, String> result = dbCollection.update(
                DBQuery.is(FIELD_NODEID, nodeId),
                new DBUpdate.Builder()
                        .set(FIELD_STATE, state),
                false,
                false);

        return result.getN() > 0;

    }

    public void deleteAll() {
        dbCollection.drop();
    }
}
