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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import static org.graylog.security.certutil.CertConstants.PKCS12;

public record FilesystemKeystoreInformation(Path location, char[] password) implements KeystoreInformation {
    public FilesystemKeystoreInformation(Path location, String password) {
        this(location, password.toCharArray());
    }

    public KeyStore loadKeystore() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance(PKCS12);
        try (FileInputStream fis = new FileInputStream(location.toFile())) {
            keyStore.load(fis, password);
            return keyStore;
        }
    }
}
