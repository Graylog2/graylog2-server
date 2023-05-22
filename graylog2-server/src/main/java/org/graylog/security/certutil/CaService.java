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
package org.graylog.security.certutil;

import org.graylog.security.certutil.ca.CACreator;
import org.graylog.security.certutil.ca.exceptions.CACreationException;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.keystore.storage.KeystoreFileStorage;
import org.graylog.security.certutil.keystore.storage.KeystoreMongoStorage;
import org.graylog2.bootstrap.preflight.web.resources.model.CA;
import org.graylog2.bootstrap.preflight.web.resources.model.CAType;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Optional;

@Singleton
public class CaService {
    private static final Logger LOG = LoggerFactory.getLogger(CaService.class);
    public static final String keystoreFilename = "graylog-ca.p12";
    private final KeystoreFileStorage keystoreFileStorage;
    private final KeystoreMongoStorage keystoreMongoStorage;
    private final NodeId nodeId;
    private final CACreator caCreator;

    private Optional<CA> currentCA = Optional.empty();

    private Optional<String> password;
    private Optional<Path> caKeystoreFile;

    @Inject
    public CaService(final CaConfiguration configuration,
                     final KeystoreFileStorage keystoreFileStorage,
                     final KeystoreMongoStorage keystoreMongoStorage,
                     final NodeId nodeId,
                     final CACreator caCreator) {
        this.keystoreFileStorage = keystoreFileStorage;
        this.keystoreMongoStorage = keystoreMongoStorage;
        this.nodeId = nodeId;
        this.caCreator = caCreator;

        if(configuration.getCaKeystoreFile() != null && Files.exists(configuration.getCaKeystoreFile())) {
            currentCA = Optional.of(new CA("local CA", CAType.LOCAL));
            caKeystoreFile = Optional.of(configuration.getCaKeystoreFile());
        }
        password = Optional.ofNullable(configuration.getCaPassword());
    }

    public CA get() {
        return currentCA.get();
    }


    public CA create(String password) throws CACreationException {
        if(password != null) {
            this.password = Optional.of(password);
        }

        try {
            final var pass = this.password.orElse("").toCharArray();
            final Duration certificateValidity = Duration.ofDays(10 * 365);
            KeyStore keyStore = caCreator.createCA(pass, certificateValidity);
            keystoreMongoStorage.writeKeyStore(nodeId, keyStore, pass);
        } catch (Exception ex) {
            LOG.error("Could not generate CA: " + ex.getMessage(), ex);
            throw new CACreationException("Could not generate CA: " + ex.getMessage(), ex);
        }

        currentCA = Optional.of(new CA("generated CA", CAType.GENERATED));
        return currentCA.get();
    }

    public CA upload(String password, String ca) throws CACreationException {
        try {
            this.caKeystoreFile = Optional.of(Path.of(keystoreFilename));
            Files.write(caKeystoreFile.get(), ca.getBytes());
            this.password = Optional.ofNullable(password);
            final var pass = this.password.orElse("").toCharArray();
            KeyStore keyStore = caCreator.uploadCA(pass, ca);
            keystoreMongoStorage.writeKeyStore(nodeId, keyStore, pass);
        } catch (IOException | KeyStoreStorageException ex) {
            LOG.error("Could not write CA: " + ex.getMessage(), ex);
            throw new CACreationException("Could not write CA: " + ex.getMessage(), ex);
        }
        currentCA = Optional.of(new CA("uploaded CA", CAType.UPLOADED));
        return get();
    }

    public void startOver() {
        if(currentCA.isPresent() && !currentCA.get().type().equals(CAType.LOCAL)) {
            // TODO: cleanup in MongoDB
            currentCA = Optional.empty();
        }
    }
}
