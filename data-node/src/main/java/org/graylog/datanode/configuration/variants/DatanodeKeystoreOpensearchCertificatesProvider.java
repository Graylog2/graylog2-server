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
package org.graylog.datanode.configuration.variants;

import jakarta.inject.Inject;
import org.apache.commons.lang3.RandomStringUtils;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog.datanode.configuration.DatanodeKeystoreException;
import org.graylog.datanode.configuration.OpensearchConfigurationException;
import org.graylog.security.certutil.csr.InMemoryKeystoreInformation;
import org.graylog.security.certutil.keystore.storage.KeystoreUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.function.Supplier;

public final class DatanodeKeystoreOpensearchCertificatesProvider implements OpensearchCertificatesProvider {
    private final DatanodeKeystore datanodeKeystore;

    @Inject
    public DatanodeKeystoreOpensearchCertificatesProvider(final DatanodeKeystore datanodeKeystore) {
        this.datanodeKeystore = datanodeKeystore;
    }

    @Override
    public boolean isConfigured(Configuration localConfiguration) {
        try {
            return datanodeKeystore.exists() && datanodeKeystore.hasSignedCertificate();
        } catch (DatanodeKeystoreException e) {
            throw new OpensearchConfigurationException(e);
        }
    }

    @Override
    public OpensearchCertificates build() {
        final char[] oneTimePassword = RandomStringUtils.secure().nextAlphabetic(256).toCharArray();

        Supplier<KeyStore> keyStoreSupplier = createKeystoreSupplier(oneTimePassword);
        return new OpensearchCertificates(oneTimePassword, keyStoreSupplier, null, keyStoreSupplier, null);
    }

    private Supplier<KeyStore> createKeystoreSupplier(char[] oneTimePassword) {
        return () -> {
            try {
                final InMemoryKeystoreInformation safeCopy = this.datanodeKeystore.getSafeCopy();
                return KeystoreUtils.newStoreCopyContent(safeCopy.loadKeystore(), safeCopy.password(), oneTimePassword);
            } catch (DatanodeKeystoreException | GeneralSecurityException | IOException e) {
                throw new RuntimeException("Failed to obtain datanode keystore", e);
            }
        };
    }
}
