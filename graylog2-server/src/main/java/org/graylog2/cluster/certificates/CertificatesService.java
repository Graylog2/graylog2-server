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
package org.graylog2.cluster.certificates;

import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.indices.MongoDbIndexTools;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.mongojack.DBQuery;
import org.mongojack.DBUpdate;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import java.util.Optional;

import static org.graylog2.cluster.certificates.NodeCertificate.FIELD_ENCR_CERTIFICATE;
import static org.graylog2.cluster.certificates.NodeCertificate.FIELD_NODEID;


public class CertificatesService extends PaginatedDbService<NodeCertificate> {
    public static final String COLLECTION_NAME = "data_node_certificates";

    private final JacksonDBCollection<NodeCertificate, String> dbCollection;
    private final EncryptedValueService encryptionService;

    @Inject
    public CertificatesService(final MongoJackObjectMapperProvider mongoJackObjectMapperProvider,
                               final MongoConnection mongoConnection,
                               final EncryptedValueService encryptionService) {
        super(mongoConnection, mongoJackObjectMapperProvider, NodeCertificate.class, COLLECTION_NAME);
        this.dbCollection = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(COLLECTION_NAME), NodeCertificate.class, String.class, mongoJackObjectMapperProvider.get());
        new MongoDbIndexTools(db).createUniqueIndex(FIELD_NODEID);
        this.encryptionService = encryptionService;

    }

    public boolean writeCert(final String nodeId,
                             final String cert) {
        final EncryptedValue encrypted = encryptionService.encrypt(cert);
        final WriteResult<NodeCertificate, String> result = dbCollection.update(
                DBQuery.is(FIELD_NODEID, nodeId),
                DBUpdate
                        .set(FIELD_NODEID, nodeId)
                        .set(FIELD_ENCR_CERTIFICATE, encrypted),
                true,
                false
        );
        return result.getN() > 0;
    }

    public Optional<String> readCert(final String nodeId) {
        final NodeCertificate nod = dbCollection.findOneById(nodeId);
        if (nod != null && nod.encryptedCertificate() != null) {
            return Optional.ofNullable(encryptionService.decrypt(nod.encryptedCertificate()));
        } else {
            return Optional.empty();
        }
    }
}
