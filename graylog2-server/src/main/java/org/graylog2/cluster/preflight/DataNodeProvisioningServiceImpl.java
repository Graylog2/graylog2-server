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

import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

import static org.graylog2.cluster.preflight.DataNodeProvisioningConfig.FIELD_CERTIFICATE;
import static org.graylog2.cluster.preflight.DataNodeProvisioningConfig.FIELD_CSR;
import static org.graylog2.cluster.preflight.DataNodeProvisioningConfig.FIELD_NODEID;
import static org.graylog2.cluster.preflight.DataNodeProvisioningConfig.FIELD_STATE;
import static org.graylog2.cluster.preflight.DataNodeProvisioningConfig.State.CSR;
import static org.graylog2.cluster.preflight.DataNodeProvisioningConfig.State.SIGNED;

public class DataNodeProvisioningServiceImpl extends PaginatedDbService<DataNodeProvisioningConfig> implements DataNodeProvisioningService {
    public static final String COLLECTION_NAME = "datanode_provisioning_config";

    private final JacksonDBCollection<DataNodeProvisioningConfig, String> dbCollection;

    @Inject
    public DataNodeProvisioningServiceImpl(final MongoJackObjectMapperProvider mongoJackObjectMapperProvider,
                                           final MongoConnection mongoConnection) {
        super(mongoConnection, mongoJackObjectMapperProvider, DataNodeProvisioningConfig.class, COLLECTION_NAME);
        this.dbCollection = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(COLLECTION_NAME), DataNodeProvisioningConfig.class, String.class, mongoJackObjectMapperProvider.get());
        new MongoDbIndexTools(db).createUniqueIndex(FIELD_NODEID);
    }

    @Override
    public Optional<DataNodeProvisioningConfig> getPreflightConfigFor(String nodeId) {
        return Optional.ofNullable(dbCollection.findOne(DBQuery.is(FIELD_NODEID, nodeId)));
    }

    @Override
    public List<DataNodeProvisioningConfig> findAllNodesThatNeedAttention() {
        return this.streamQuery(DBQuery.notIn(FIELD_STATE, DataNodeProvisioningConfig.State.CONNECTED)).toList();
    }

    @Override
    public void writeCsr(final String nodeId,
                         final String csr) {
        final WriteResult<DataNodeProvisioningConfig, String> result = dbCollection.update(
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
        final WriteResult<DataNodeProvisioningConfig, String> result = dbCollection.update(
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
        final DataNodeProvisioningConfig config = dbCollection.findOne(
                DBQuery.is(FIELD_NODEID, nodeId)
        );
        if (config != null) {
            return Optional.ofNullable(config.certificate());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void changeState(final String nodeId, final DataNodeProvisioningConfig.State state) {
        final WriteResult<DataNodeProvisioningConfig, String> result = dbCollection.update(
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
