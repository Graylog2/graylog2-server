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
package org.graylog.security.certutil.csr.storage;

import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.graylog2.cluster.NodePreflightConfig;
import org.graylog2.cluster.NodePreflightConfigService;
import org.graylog2.plugin.system.NodeId;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

public class CsrMongoStorage implements CsrStorage {

    private NodePreflightConfigService mongoService;
    private NodeId nodeId;

    @Inject
    public CsrMongoStorage(final NodePreflightConfigService mongoService,
                           final NodeId nodeId) {
        this.mongoService = mongoService;
        this.nodeId = nodeId;
    }

    @Override
    public void writeCsr(PKCS10CertificationRequest csr) throws IOException, OperatorCreationException {
        try (StringWriter writer = new StringWriter()) {
            try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(writer)) {
                jcaPEMWriter.writeObject(csr);
            }
            mongoService.writeCsr(nodeId.getNodeId(), writer.toString());
        }
    }

    @Override
    public PKCS10CertificationRequest readCsr() throws IOException, OperatorCreationException {
        final NodePreflightConfig preflightConfig = mongoService.getPreflightConfigFor(nodeId.getNodeId());
        final String csr = preflightConfig.csr();

        Reader pemReader = new BufferedReader(new StringReader(csr));
        PEMParser pemParser = new PEMParser(pemReader);
        Object parsedObj = pemParser.readObject();
        if (parsedObj instanceof PKCS10CertificationRequest) {
            return (PKCS10CertificationRequest) parsedObj;
        }
        return null;
    }
}
