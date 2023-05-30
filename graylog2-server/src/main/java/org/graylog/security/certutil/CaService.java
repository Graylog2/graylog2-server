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
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.graylog.security.certutil.ca.CACreator;
import org.graylog.security.certutil.ca.exceptions.CACreationException;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.keystore.storage.KeystoreFileStorage;
import org.graylog2.Configuration;
import org.graylog2.bootstrap.preflight.web.resources.model.CA;
import org.graylog2.bootstrap.preflight.web.resources.model.CAType;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
    private final KeystoreFileStorage keystoreFileStorage;
    private final NodeId nodeId;
    private final CACreator caCreator;
    private final CaConfiguration configuration;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public CaService(final Configuration configuration,
                     final KeystoreFileStorage keystoreFileStorage,
                     final NodeId nodeId,
                     final CACreator caCreator,
                     final ClusterConfigService clusterConfigService) {
        this.keystoreFileStorage = keystoreFileStorage;
        this.nodeId = nodeId;
        this.caCreator = caCreator;
        this.configuration = configuration;
        this.clusterConfigService = clusterConfigService;
    }

    private boolean configuredCaExists() {
        return configuration.getCaKeystoreFile() != null && Files.exists(configuration.getCaKeystoreFile());
    }

    public CA get() {
        if(configuredCaExists()) {
            return new CA("local CA", CAType.LOCAL);
        } else {
            var config = clusterConfigService.get(CaClusterConfig.class);
            return config != null ? new CA(config.id(), config.type()) : null;
        }
    }

    public void create(final Integer daysValid, char[] password) throws CACreationException {
        final Duration certificateValidity = Duration.ofDays(daysValid == null || daysValid == 0 ? DEFAULT_VALIDITY: daysValid);
        KeyStore keyStore = caCreator.createCA(null, certificateValidity);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            keyStore.store(baos, password);
            final String keystoreDataAsString = java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
            var config = new CaClusterConfig("generated CA", CAType.GENERATED, keystoreDataAsString);
            clusterConfigService.write(config);
        } catch (Exception ex) {
            throw new CACreationException("Failed to save keystore cluster config", ex);
        }
    }

    public void upload(String pass, FormDataMultiPart params) throws CACreationException {
        final var password = pass == null ? null : pass.toCharArray();
        // TODO: if the upload consists of more than one file, handle accordingly
        // or: decide that it's always only one file containing all certificates
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            List<FormDataBodyPart> parts = params.getFields("file");
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
            keyStore.store(baos, password);
            final String keystoreDataAsString = java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
            var config = new CaClusterConfig("generated CA", CAType.GENERATED, keystoreDataAsString);
            clusterConfigService.write(config);
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException |
                 CertificateException ex) {
            LOG.error("Could not write CA: " + ex.getMessage(), ex);
            throw new CACreationException("Could not write CA: " + ex.getMessage(), ex);
        }
    }

    public void startOver() {

    }

    public Optional<KeyStore> loadKeyStore(char[] password) throws KeyStoreException, KeyStoreStorageException, NoSuchAlgorithmException {
        if(configuredCaExists()) {
            return Optional.of(keystoreFileStorage.readKeyStore(configuration.getCaKeystoreFile(), null).orElseThrow());
        } else {
            var config = clusterConfigService.get(CaClusterConfig.class);
            if (config != null) {
                try (ByteArrayInputStream bais = new ByteArrayInputStream(java.util.Base64.getDecoder().decode(config.keystore()))) {
                    KeyStore keyStore = KeyStore.getInstance(PKCS12);
                    keyStore.load(bais, password);
                    return Optional.of(keyStore);
                } catch (Exception ex) {
                    throw new KeyStoreStorageException("Failed to load keystore cluster config", ex);
                }
            } else {
                return Optional.empty();
            }
        }
    }
}
