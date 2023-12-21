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

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.OperatorCreationException;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog2.cluster.preflight.DataNodeProvisioningService;

import jakarta.inject.Inject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class CertChainMongoStorage implements CertChainStorage {

    private DataNodeProvisioningService mongoService;

    @Inject
    public CertChainMongoStorage(final DataNodeProvisioningService mongoService) {
        this.mongoService = mongoService;
    }

    @Override
    public void writeCertChain(final CertificateChain certChain,
                               final String nodeId) throws IOException, OperatorCreationException {
        var writer = new StringWriter();
        try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(writer)) {
            for (Certificate cert : certChain.toCertificateChainArray()) {
                jcaPEMWriter.writeObject(cert);
            }

        }
        mongoService.writeCert(nodeId, writer.toString());
    }

    @Override
    public Optional<CertificateChain> readCertChain(final String nodeId) throws IOException, GeneralSecurityException {
        final Optional<String> certAsString = mongoService.readCert(nodeId);

        if (certAsString.isPresent()) {
            Reader pemReader = new BufferedReader(new StringReader(certAsString.get()));
            PEMParser pemParser = new PEMParser(pemReader);
            List<X509Certificate> caCerts = new LinkedList<>();
            X509Certificate signedCert = readSingleCert(pemParser);

            if (signedCert != null) {
                X509Certificate caCert = readSingleCert(pemParser);
                while (caCert != null) {
                    caCerts.add(caCert);
                    caCert = readSingleCert(pemParser);
                }
                return Optional.of(new CertificateChain(signedCert, caCerts));
            }
        }
        return Optional.empty();
    }

    private X509Certificate readSingleCert(PEMParser pemParser) throws IOException, CertificateException {
        var parsedObj = pemParser.readObject();
        if (parsedObj instanceof X509Certificate) {
            return (X509Certificate) parsedObj;
        } else if (parsedObj instanceof X509CertificateHolder) {
            return new JcaX509CertificateConverter().getCertificate(
                    (X509CertificateHolder) parsedObj
            );
        }
        return null;
    }
}
