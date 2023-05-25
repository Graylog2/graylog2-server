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

import org.apache.commons.net.util.Base64;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.graylog.security.certutil.ca.CACreator;
import org.graylog.security.certutil.ca.exceptions.CACreationException;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.keystore.storage.KeystoreFileStorage;
import org.graylog.security.certutil.keystore.storage.KeystoreMongoStorage;
import org.graylog2.Configuration;
import org.graylog2.bootstrap.preflight.web.resources.model.CA;
import org.graylog2.bootstrap.preflight.web.resources.model.CAType;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Optional;

import static org.graylog.security.certutil.CertConstants.KEYSTORE_TYPE;

@Singleton
public class CaService {
    private static final Logger LOG = LoggerFactory.getLogger(CaService.class);
    // TODO: clarify default value
    private static int DEFAULT_VALIDITY = 10 * 365;
    private final KeystoreFileStorage keystoreFileStorage;
    private final KeystoreMongoStorage keystoreMongoStorage;
    private final NodeId nodeId;
    private final CACreator caCreator;
    private final CaConfiguration configuration;

    private Optional<CA> currentCA = Optional.empty();

    private Optional<String> password;

    @Inject
    public CaService(final Configuration configuration,
                     final KeystoreFileStorage keystoreFileStorage,
                     final KeystoreMongoStorage keystoreMongoStorage,
                     final NodeId nodeId,
                     final CACreator caCreator) {
        this.keystoreFileStorage = keystoreFileStorage;
        this.keystoreMongoStorage = keystoreMongoStorage;
        this.nodeId = nodeId;
        this.caCreator = caCreator;
        this.configuration = configuration;

        if(configuration.getCaKeystoreFile() != null && Files.exists(configuration.getCaKeystoreFile())) {
            currentCA = Optional.of(new CA("local CA", CAType.LOCAL));
        }
        password = Optional.ofNullable(configuration.getCaPassword());
    }

    public CA get() {
        return currentCA.get();
    }

    // TODO: write local CA and password / also password for generated/uploaded CA into MongoDB

    public CA create(final Integer daysValid) throws CACreationException {
        try {
            final var pass = this.password.map(String::toCharArray).orElse(null);
            final Duration certificateValidity = Duration.ofDays(daysValid == null || daysValid == 0 ? DEFAULT_VALIDITY: daysValid);
            KeyStore keyStore = caCreator.createCA(pass, certificateValidity);
            keystoreMongoStorage.writeKeyStore(nodeId, keyStore, pass);
        } catch (Exception ex) {
            LOG.error("Could not generate CA: " + ex.getMessage(), ex);
            throw new CACreationException("Could not generate CA: " + ex.getMessage(), ex);
        }

        this.currentCA = Optional.of(new CA("generated CA", CAType.GENERATED));
        return this.currentCA.get();
    }

    public CA upload(String password, FormDataBodyPart body) throws CACreationException {
        // TODO: if the upload consists of more than one file, handle accordingly
        // or: decide that it's always only one file containing all certificates
        try {
            this.password = Optional.ofNullable(password);
            final var pass = this.password.map(String::toCharArray).orElse(null);

            // for(BodyPart part : body.getParent().getBodyParts())
            BodyPart part = body.getParent().getBodyParts().get(0);
            InputStream is = part.getEntityAs(InputStream.class);
            ContentDisposition meta = part.getContentDisposition();
            String pem = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            // TODO: combine all parts into one keystore etc.
            // }
            String base64 = new String(new Base64().decode(pem), StandardCharsets.UTF_8);
            if(base64.contains("-----BEGIN CERTIFICATE-----")) {
                KeyStore keyStore = caCreator.uploadCA(pass, pem);
                keystoreMongoStorage.writeKeyStore(nodeId, keyStore, pass);
            } else {
                ByteArrayInputStream bais = new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8));
                KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
                keyStore.load(bais, pass);
                keystoreMongoStorage.writeKeyStore(nodeId, keyStore, pass);
            }
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | KeyStoreStorageException |
                 CertificateException ex) {
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

    public Optional<KeyStore> loadKeyStore() throws KeyStoreException, KeyStoreStorageException, NoSuchAlgorithmException {
        if(currentCA.isPresent()) {
            final var pass = this.password.map(String::toCharArray).orElse(null);
            if(currentCA.get().type().equals(CAType.LOCAL)) {
                return Optional.of(keystoreFileStorage.readKeyStore(configuration.getCaKeystoreFile(), pass).orElseThrow());
            } else {
                return Optional.of(keystoreMongoStorage.readKeyStore(nodeId, pass).orElseThrow());
            }
        }
        return Optional.empty();
    }
}
