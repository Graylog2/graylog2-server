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

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.graylog.security.certutil.ca.CAKeyPair;
import org.graylog.security.certutil.ca.PemCaReader;
import org.graylog.security.certutil.ca.exceptions.CACreationException;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog2.Configuration;
import org.graylog2.bootstrap.preflight.web.resources.model.CAType;
import org.graylog2.bootstrap.preflight.web.resources.model.CertificateAuthorityInformation;
import org.graylog2.cluster.certificates.EncryptedCaKeystore;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.security.encryption.EncryptedValueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.graylog.security.certutil.CertConstants.CA_KEY_ALIAS;
import static org.graylog.security.certutil.CertConstants.PKCS12;

@Singleton
class CaPersistenceService {

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }


    private static final Logger LOG = LoggerFactory.getLogger(CaPersistenceService.class);

    public static final String CA_KEYSTORE_ID = "GRAYLOG CA";

    private final CaConfiguration configuration;

    private final String passwordSecret;

    private final ClusterEventBus eventBus;

    private final ClusterConfigService clusterConfigService;

    private final EncryptedValueService encryptionService;

    @Inject
    public CaPersistenceService(final Configuration configuration,
                                final @Named("password_secret") String passwordSecret,
                                ClusterEventBus eventBus,
                                ClusterConfigService clusterConfigService,
                                EncryptedValueService encryptionService) {
        this.configuration = configuration;
        this.clusterConfigService = clusterConfigService;
        this.encryptionService = encryptionService;
        this.passwordSecret = passwordSecret;
        this.eventBus = eventBus;
    }

    public Optional<CertificateAuthorityInformation> get() throws KeyStoreStorageException {
        if (configuration.configuredCaExists()) {
            return Optional.of(new CertificateAuthorityInformation("local CA", CAType.LOCAL));
        } else {
            return readFromDatabase()
                    .map(c -> new CertificateAuthorityInformation(CA_KEYSTORE_ID, CAType.GENERATED));
        }
    }

    public CertificateAuthorityInformation create(final String organization, final Integer daysValid) throws CACreationException, KeyStoreStorageException, KeyStoreException {
        final Duration certificateValidity = Duration.ofDays(daysValid == null || daysValid == 0 ? CaKeystore.DEFAULT_SELFSIGNED_VALIDITY_DAYS : daysValid);
        KeyStore keyStore = CAKeyPair.create(organization, passwordSecret.toCharArray(), certificateValidity).toKeyStore();
        writeToDatabase(keyStore);
        LOG.debug("Generated a new CA.");
        triggerCaChangedEvent();
        return get().orElseThrow(() -> new IllegalStateException("Failed to obtain CA information, but a CA has been just created. Inconsistent state!"));
    }

    public void upload(@Nullable String password, List<FormDataBodyPart> parts) throws CACreationException {
        final var providedPassword = password == null ? null : password.toCharArray();
        // TODO: if the upload consists of more than one file, handle accordingly
        // or: decide that it's always only one file containing all certificates
        try {
            KeyStore keyStore = KeyStore.getInstance(PKCS12, "BC");
            keyStore.load(null, null);
            for (BodyPart part : parts) {
                InputStream is = part.getEntityAs(InputStream.class);
                byte[] bytes = is.readAllBytes();
                String pem = new String(bytes, StandardCharsets.UTF_8);
                // Test, if upload is PEM file, must contain at least a certificate
                if (pem.contains("-----BEGIN CERTIFICATE")) {
                    var ca = PemCaReader.readCA(pem, password);
                    keyStore.setKeyEntry(CA_KEY_ALIAS, ca.privateKey(), providedPassword, ca.certificates().toArray(new Certificate[0]));
                } else {
                    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                    keyStore.load(bais, providedPassword);
                }
            }
            writeToDatabase(adaptUploadedKeystore(keyStore, providedPassword));
            triggerCaChangedEvent();
        } catch (IOException | KeyStoreStorageException | GeneralSecurityException ex) {
            LOG.error("Could not write CA: " + ex.getMessage(), ex);
            throw new CACreationException("Could not write CA: " + ex.getMessage(), ex);
        }
    }

    /**
     * we want to get rid of the provided uploaded password and replace it with our general password secret, to standardize
     * all usages - for both uploaded and generated self-signed keystores. Additionally we want to unify aliases
     */
    @Nonnull
    private KeyStore adaptUploadedKeystore(KeyStore existingKeystore, char[] existingPassword) throws GeneralSecurityException, IOException {
        KeyStore adaptedKeystore = KeyStore.getInstance(PKCS12);
        adaptedKeystore.load(null, passwordSecret.toCharArray());
        final ArrayList<String> aliases = Collections.list(existingKeystore.aliases());
        if(aliases.isEmpty()) {
            throw new IllegalStateException("Provided keystore is empty!");
        } else if (aliases.size() == 1) {
            final String alias = aliases.iterator().next();
            if (existingKeystore.isKeyEntry(alias)) {
                LOG.info("Found one alias " + alias + ", extracting and persisting key and certificate chain.");
                final Key key = existingKeystore.getKey(alias, existingPassword);
                final Certificate[] certChain = existingKeystore.getCertificateChain(alias);
                adaptedKeystore.setKeyEntry(CA_KEY_ALIAS, key, passwordSecret.toCharArray(), certChain);
            } else {
                throw new IllegalStateException("Only one alias " + alias + " found in the keystore and it doesn't have a key assigned");
            }
        } else {
            // TODO: handle situations with multiple aliases
            throw new IllegalStateException("Keystores with multiple keys not supported yet!");
        }
        return adaptedKeystore;
    }

    public void startOver() {
        clusterConfigService.remove(EncryptedCaKeystore.class);
    }


    private void triggerCaChangedEvent() {
        eventBus.post(new CertificateAuthorityChangedEvent());
    }

    public Optional<CaKeystoreWithPassword> loadKeyStore() throws KeyStoreStorageException {
        if (configuration.configuredCaExists()) {
            // TODO: we could cache this for better performance
            return readFromFS();
        } else {
            return readFromDatabase();
        }
    }

    private void writeToDatabase(KeyStore keyStore) throws KeyStoreStorageException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            keyStore.store(baos, passwordSecret.toCharArray());
            final String keystoreDataAsString = Base64.getEncoder().encodeToString(baos.toByteArray());
            clusterConfigService.write(new EncryptedCaKeystore(encryptionService.encrypt(keystoreDataAsString)));
        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException e) {
            throw new KeyStoreStorageException("Failed to save keystore to cluster config service", e);
        }
    }

    private Optional<CaKeystoreWithPassword> readFromDatabase() {
        return Optional.ofNullable(clusterConfigService.get(EncryptedCaKeystore.class))
                .map(EncryptedCaKeystore::keystore)
                .map(encryptionService::decrypt)
                .map(Base64.getDecoder()::decode)
                .map(this::parseKEystoreFromString)
                .map(ks -> new CaKeystoreWithPassword(ks, passwordSecret));
    }

    @Nonnull
    private KeyStore parseKEystoreFromString(byte[] keystoreAsString) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(keystoreAsString)) {
            KeyStore keyStore = KeyStore.getInstance(PKCS12);
            keyStore.load(bais, passwordSecret.toCharArray());
            return keyStore;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load keystore from Mongo collection", ex);
        }
    }

    private Optional<CaKeystoreWithPassword> readFromFS() throws KeyStoreStorageException {
        try (var in = Files.newInputStream(configuration.getCaKeystoreFile())) {
            KeyStore caKeystore = KeyStore.getInstance(CertConstants.PKCS12);
            caKeystore.load(in, configuration.getCaPassword().toCharArray());
            return Optional.of(caKeystore).map(ks -> new CaKeystoreWithPassword(ks, configuration.getCaPassword()));
        } catch (IOException | GeneralSecurityException ex) {
            throw new KeyStoreStorageException("Could not read keystore: " + ex.getMessage(), ex);
        }
    }
}
