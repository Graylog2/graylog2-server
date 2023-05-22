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
import org.bouncycastle.operator.OperatorException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.graylog2.cluster.NodePreflightConfig;
import org.graylog2.cluster.NodePreflightConfigService;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Optional;

public class CsrMongoStorage {

    private NodePreflightConfigService mongoService;

    @Inject
    public CsrMongoStorage(final NodePreflightConfigService mongoService) {
        this.mongoService = mongoService;
    }

    public void writeCsr(PKCS10CertificationRequest csr, String nodeId) throws IOException, OperatorException {
        try (StringWriter writer = new StringWriter()) {
            try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(writer)) {
                jcaPEMWriter.writeObject(csr);
            }
            mongoService.writeCsr(nodeId, writer.toString());
        }
    }


    public Optional<PKCS10CertificationRequest> readCsr(String nodeId) throws IOException, OperatorException {
        final NodePreflightConfig preflightConfig = mongoService.getPreflightConfigFor(nodeId);
        if (preflightConfig != null) {
            final String csr = preflightConfig.csr();

            if (csr != null) {
                Reader pemReader = new BufferedReader(new StringReader(csr));
                PEMParser pemParser = new PEMParser(pemReader);
                Object parsedObj = pemParser.readObject();
                if (parsedObj instanceof PKCS10CertificationRequest) {
                    return Optional.of((PKCS10CertificationRequest) parsedObj);
                }
            }
        }
        return Optional.empty();
    }
}
