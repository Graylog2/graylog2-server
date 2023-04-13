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
package org.graylog.security.certutil.csr;

import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.security.Security;

public record CsrFileStorage(String csrFilename) implements CsrStorage {

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    @Override
    public void writeCsr(PKCS10CertificationRequest csr)
            throws IOException, OperatorCreationException {

        try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(new FileWriter(csrFilename))) {
            jcaPEMWriter.writeObject(csr);
        }
    }

    @Override
    public PKCS10CertificationRequest readCsr()
            throws IOException, OperatorCreationException {

        Reader pemReader = new BufferedReader(new FileReader(csrFilename));
        PEMParser pemParser = new PEMParser(pemReader);
        Object parsedObj = pemParser.readObject();
        if (parsedObj instanceof PKCS10CertificationRequest) {
            return (PKCS10CertificationRequest) parsedObj;
        }
        return null;
    }
}
