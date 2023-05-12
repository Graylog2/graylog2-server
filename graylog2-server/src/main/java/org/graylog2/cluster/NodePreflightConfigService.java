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

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.graylog.security.certutil.csr.CsrGenerator;
import org.graylog.security.certutil.csr.storage.CsrMongoStorage;
import org.graylog.security.certutil.csr.storage.CsrStorage;
import org.graylog.security.certutil.privatekey.PrivateKeyEncryptedFileStorage;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.plugin.system.NodeId;
import org.mongojack.DBQuery;
import org.mongojack.DBUpdate;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Objects;

import static org.graylog2.cluster.NodePreflightConfig.FIELD_CSR;
import static org.graylog2.cluster.NodePreflightConfig.FIELD_NODEID;
import static org.graylog2.cluster.NodePreflightConfig.FIELD_STATE;
import static org.graylog2.cluster.NodePreflightConfig.State.CSR;

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
        this.csrGenerator = csrGenerator;
    }

    public NodePreflightConfig getPreflightConfigFor(Node node) {
        return getPreflightConfigFor(node.getNodeId());
    }

    public NodePreflightConfig getPreflightConfigFor(String nodeId) {
        return dbCollection.findOne(DBQuery.is(FIELD_NODEID, nodeId));
    }

    public void updatePreflightConfig(final NodeId nodeId) {
        final NodePreflightConfig preflightConfigFor = getPreflightConfigFor(nodeId.getNodeId());
        if (preflightConfigFor == null) {
            dbCollection.insert(NodePreflightConfig.builder()
                    .nodeId(nodeId.getNodeId())
                    .state(NodePreflightConfig.State.UNCONFIGURED)
                    .build());
        } else if (Objects.equals(preflightConfigFor.state(), NodePreflightConfig.State.CONFIGURED) && preflightConfigFor.csr() == null) {
            try {
                //TODO: most of the parameters are hardcoded, so that we can just check if CSR is generated and stored in Mongo
                //TODO: the details will be changed in one of the next PRs
                final PKCS10CertificationRequest csr = csrGenerator.generateCSR(
                        "changeMe".toCharArray(),
                        "localhost",
                        List.of("data-node"),
                        new PrivateKeyEncryptedFileStorage("tbd.key"));
                final CsrStorage csrStorage = new CsrMongoStorage(this, nodeId);
                csrStorage.writeCsr(csr);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

    public void deleteAll() {
        dbCollection.drop();
    }
}
