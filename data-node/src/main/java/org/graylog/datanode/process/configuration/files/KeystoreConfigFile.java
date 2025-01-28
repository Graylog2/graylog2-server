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
package org.graylog.datanode.process.configuration.files;

import org.graylog.datanode.configuration.OpensearchConfigurationException;
import org.graylog.security.certutil.csr.KeystoreInformation;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

public record KeystoreConfigFile(Path relativePath, KeystoreInformation keystoreInformation) implements DatanodeConfigFile {

    @Override
    public void write(OutputStream stream) throws IOException {
        try {
            keystoreInformation().loadKeystore().store(stream, keystoreInformation.password());
        } catch (Exception e) {
            throw new OpensearchConfigurationException("Failed to persist opensearch keystore file " + relativePath, e);
        }
    }
}
