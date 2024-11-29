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
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog.datanode.configuration.DatanodeKeystoreException;
import org.graylog.datanode.configuration.OpensearchConfigurationException;
import org.graylog.security.certutil.csr.InMemoryKeystoreInformation;

public final class LocalKeystoreSecureConfiguration implements SecurityConfigurationVariant {
    private final DatanodeKeystore datanodeKeystore;

    @Inject
    public LocalKeystoreSecureConfiguration(final DatanodeKeystore datanodeKeystore) {
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
    public OpensearchSecurityConfiguration build() {
        try {
            final InMemoryKeystoreInformation reencryptedCopy = this.datanodeKeystore.getSafeCopy();
            return new OpensearchSecurityConfiguration(reencryptedCopy, reencryptedCopy);
        } catch (DatanodeKeystoreException e) {
            throw new OpensearchConfigurationException(e);
        }
    }
}
