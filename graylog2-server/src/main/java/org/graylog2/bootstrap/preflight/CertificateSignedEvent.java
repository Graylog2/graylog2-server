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
package org.graylog2.bootstrap.preflight;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.graylog.security.certutil.cert.CertificateChain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

public record CertificateSignedEvent(String nodeId, String certificate) {
    public static CertificateSignedEvent fromCertChain(String nodeId, CertificateChain certChain) throws IOException {
        var writer = new StringWriter();
        try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(writer)) {
            for (Certificate c : certChain.toCertificateChainArray()) {
                jcaPEMWriter.writeObject(c);
            }
        }
        return new CertificateSignedEvent(nodeId, writer.toString());
    }

    @JsonIgnore
    public CertificateChain readCertChain() throws CertificateException, IOException {
        Reader pemReader = new BufferedReader(new StringReader(certificate));
        PEMParser pemParser = new PEMParser(pemReader);
        List<X509Certificate> caCerts = new LinkedList<>();
        X509Certificate signedCert = readSingleCert(pemParser);

        X509Certificate caCert = readSingleCert(pemParser);
        while (caCert != null) {
            caCerts.add(caCert);
            caCert = readSingleCert(pemParser);
        }
        return new CertificateChain(signedCert, caCerts);
    }

    private X509Certificate readSingleCert(PEMParser pemParser) throws IOException, CertificateException {
        var parsedObj = pemParser.readObject();
        if (parsedObj == null) {
            return null;
        }
        if (parsedObj instanceof X509Certificate) {
            return (X509Certificate) parsedObj;
        } else if (parsedObj instanceof X509CertificateHolder) {
            return new JcaX509CertificateConverter().getCertificate(
                    (X509CertificateHolder) parsedObj
            );
        } else {
            throw new IllegalArgumentException("Cannot read certificate from PEMParser. Containing object is of unexpected type " + parsedObj.getClass());
        }
    }
}
