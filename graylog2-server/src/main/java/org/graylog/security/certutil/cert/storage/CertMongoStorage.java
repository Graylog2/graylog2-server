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
package org.graylog.security.certutil.cert.storage;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.OperatorCreationException;
import org.graylog2.cluster.NodePreflightConfigService;
import org.graylog2.plugin.system.NodeId;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.X509Certificate;

public class CertMongoStorage implements CertStorage {

    private NodePreflightConfigService mongoService;
    private NodeId nodeId;

    @Inject
    public CertMongoStorage(final NodePreflightConfigService mongoService,
                           final NodeId nodeId) {
        this.mongoService = mongoService;
        this.nodeId = nodeId;
    }

    public void writeCert(X509Certificate cert) throws IOException, OperatorCreationException {
        var writer = new StringWriter();
        try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(writer)) {
            jcaPEMWriter.writeObject(cert);
        }
        mongoService.writeCert(nodeId.getNodeId(), writer.toString());
    }
}
