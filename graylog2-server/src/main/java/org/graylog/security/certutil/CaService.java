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
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.graylog.security.certutil.ca.CACreator;
import org.graylog.security.certutil.ca.exceptions.CACreationException;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.keystore.storage.SmartKeystoreStorage;
import org.graylog.security.certutil.keystore.storage.location.KeystoreFileLocation;
import org.graylog.security.certutil.keystore.storage.location.KeystoreMongoCollections;
import org.graylog.security.certutil.keystore.storage.location.KeystoreMongoLocation;
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
import java.util.List;
import java.util.Optional;

import static org.graylog.security.certutil.CertConstants.PKCS12;


@Singleton
public class CaService {
    private static final Logger LOG = LoggerFactory.getLogger(CaService.class);
    public static int DEFAULT_VALIDITY = 10 * 365;
    private final SmartKeystoreStorage keystoreStorage;
    private final KeystoreMongoLocation keystoreMongoLocation;
    private final KeystoreFileLocation keystoreFileLocation;
    private final NodeId nodeId;
    private final CACreator caCreator;
    private final CaConfiguration configuration;

    @Inject
    public CaService(final Configuration configuration,
                     final SmartKeystoreStorage keystoreStorage,
                     final NodeId nodeId,
                     final CACreator caCreator) {
        this.keystoreStorage = keystoreStorage;
        this.nodeId = nodeId;
        this.caCreator = caCreator;
        this.configuration = configuration;
        this.keystoreMongoLocation = new KeystoreMongoLocation(nodeId.getNodeId(), KeystoreMongoCollections.GRAYLOG_CA_KEYSTORE_COLLECTION);
        this.keystoreFileLocation = new KeystoreFileLocation(configuration.getCaKeystoreFile());
    }

    private boolean configuredCaExists() {
        return configuration.getCaKeystoreFile() != null && Files.exists(configuration.getCaKeystoreFile());
    }

    public CA get() throws KeyStoreStorageException {
        if(configuredCaExists()) {
            return new CA("local CA", CAType.LOCAL);
        } else {
            var keystore = keystoreStorage.readKeyStore(keystoreMongoLocation, null);
            return keystore.map(c -> new CA(nodeId.getNodeId(), CAType.GENERATED)).orElse(null);
        }
    }

    public void create(final Integer daysValid, char[] password) throws CACreationException, KeyStoreStorageException {
        final Duration certificateValidity = Duration.ofDays(daysValid == null || daysValid == 0 ? DEFAULT_VALIDITY: daysValid);
        KeyStore keyStore = caCreator.createCA(null, certificateValidity);
        keystoreStorage.writeKeyStore(keystoreMongoLocation, keyStore, null, password);
        LOG.debug("Generated a new CA.");
    }

    public void upload(String pass, List<FormDataBodyPart> parts) throws CACreationException {
        final var password = pass == null ? null : pass.toCharArray();
        // TODO: if the upload consists of more than one file, handle accordingly
        // or: decide that it's always only one file containing all certificates
        try {
            KeyStore keyStore = KeyStore.getInstance(PKCS12);
            for(BodyPart part : parts) {
                InputStream is = part.getEntityAs(InputStream.class);
                byte[] bytes = is.readAllBytes();
                String pem = new String(bytes, StandardCharsets.UTF_8);

                String base64 = new String(new Base64().decode(pem), StandardCharsets.UTF_8);
                // Test, if upload is PEM file
                if (base64.contains("-----BEGIN CERTIFICATE-----")) {
                    caCreator.uploadCA(keyStore, password, pem);
                } else {
                    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                    keyStore.load(bais, password);
                }
            }
            keystoreStorage.writeKeyStore(keystoreMongoLocation, keyStore, password, null);
       } catch (IOException | KeyStoreStorageException | NoSuchAlgorithmException | CertificateException |
                KeyStoreException ex) {
            LOG.error("Could not write CA: " + ex.getMessage(), ex);
            throw new CACreationException("Could not write CA: " + ex.getMessage(), ex);
        }
    }

    public void startOver() {

    }

    public Optional<KeyStore> loadKeyStore(char[] password) throws KeyStoreException, KeyStoreStorageException, NoSuchAlgorithmException {
        return keystoreStorage.readKeyStore(configuredCaExists() ? keystoreFileLocation : keystoreMongoLocation, password);
     }
}
