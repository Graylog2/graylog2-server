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
package org.graylog.datanode.bootstrap.preflight;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import jakarta.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bson.Document;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.csr.CsrGenerator;
import org.graylog.security.certutil.csr.CsrSigner;
import org.graylog.security.certutil.csr.InMemoryKeystoreInformation;
import org.graylog.security.certutil.csr.exceptions.CSRGenerationException;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;
import static org.graylog.datanode.configuration.DatanodeKeystore.DATANODE_KEY_ALIAS;

class LegacyDatanodeKeystoreProviderTest {
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @BeforeEach
    void setUp() {
        mongodb.start();
    }

    @AfterEach
    void tearDown() {
        mongodb.close();
    }

    @Test
    void testReadLegacyKeystore() throws Exception {
        final MongoConnection mongoConnection = mongodb.mongoConnection();

        final String passwordSecret = "this_is_my_secret_password";
        final SimpleNodeId nodeId = new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000");
        final EncryptedValueService encryptedValueService = new EncryptedValueService(passwordSecret);

        final KeyStore keystore = createSignedKeystore(passwordSecret);
        final String keystoreStringRepresentation = keystoreToBase64(keystore, passwordSecret.toCharArray());
        writeCertToMongo(mongoConnection.getMongoDatabase(), nodeId, keystoreStringRepresentation, encryptedValueService);

        final LegacyDatanodeKeystoreProvider legacyDatanodeKeystoreProvider = new LegacyDatanodeKeystoreProvider(nodeId, passwordSecret, Mockito.mock(DatanodeConfiguration.class), mongoConnection, encryptedValueService);

        final Optional<KeyStore> legacyKeystore = legacyDatanodeKeystoreProvider.get();
        Assertions.assertThat(legacyKeystore)
                .isPresent()
                .hasValueSatisfying(keyStore -> {
                    try {
                        Assertions.assertThat(keyStore.getKey("datanode", passwordSecret.toCharArray())).isNotNull();
                        Assertions.assertThat(keyStore.getCertificateChain("datanode")).isNotNull().hasSize(2);
                    } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private static void writeCertToMongo(MongoDatabase mongoDatabase, SimpleNodeId nodeId, String keystoreStringRepresentation, EncryptedValueService encryptionService) {
        MongoCollection<Document> dbCollection = mongoDatabase.getCollection(LegacyDatanodeKeystoreProvider.LEGACY_COLLECTION_NAME);
        final EncryptedValue encrypted = encryptionService.encrypt(keystoreStringRepresentation);
        dbCollection.updateOne(
                eq("node_id", nodeId.getNodeId()),
                combine(
                        set("node_id", nodeId.getNodeId()),
                        set(LegacyDatanodeKeystoreProvider.ENCRYPTED_CERTIFICATE_FIELD + ".encrypted_value", encrypted.value()),
                        set(LegacyDatanodeKeystoreProvider.ENCRYPTED_CERTIFICATE_FIELD + ".salt", encrypted.salt())
                ),
                new UpdateOptions().upsert(true)
        );
    }

    @Nonnull
    private static KeyStore createSignedKeystore(String passwordSecret) throws Exception {
        final KeyPair keyPair = generateKeyPair();
        final KeyStore keystore = keyPair.toKeystore("datanode", passwordSecret.toCharArray());
        final CertificateChain signed = singCertChain(keystore, passwordSecret);

        Key privateKey = keystore.getKey(DATANODE_KEY_ALIAS, passwordSecret.toCharArray());
        // replace the existing self-signed certificates chain with the signed chain from the event
        keystore.setKeyEntry(DATANODE_KEY_ALIAS, privateKey, passwordSecret.toCharArray(), signed.toCertificateChainArray());
        return keystore;
    }

    private static CertificateChain singCertChain(KeyStore keystore, String passwordSecret) throws Exception {
        final PKCS10CertificationRequest csr = csr(keystore, passwordSecret);
        final CsrSigner signer = new CsrSigner();
        final KeyPair ca = CertificateGenerator.generate(CertRequest.selfSigned("Graylog CA").isCA(true).validity(Duration.ofDays(365)));
        final X509Certificate datanodeCert = signer.sign(ca.privateKey(), ca.certificate(), csr, 30);
        final CertificateChain certChain = new CertificateChain(datanodeCert, List.of(ca.certificate()));
        return certChain;
    }

    private static PKCS10CertificationRequest csr(KeyStore keystore, String passwordSecret) throws CSRGenerationException {
        final InMemoryKeystoreInformation keystoreInformation = new InMemoryKeystoreInformation(keystore, passwordSecret.toCharArray());
        return CsrGenerator.generateCSR(keystoreInformation, DATANODE_KEY_ALIAS, "my-hostname", Collections.emptyList());
    }

    @Nonnull
    private static KeyPair generateKeyPair() throws Exception {
        final CertRequest certRequest = CertRequest.selfSigned(DATANODE_KEY_ALIAS)
                .isCA(false)
                .validity(Duration.ofDays(31));
        return CertificateGenerator.generate(certRequest);
    }

    private static String keystoreToBase64(final KeyStore keyStore, char[] keystorePassword) throws KeyStoreStorageException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            keyStore.store(baos, keystorePassword);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception ex) {
            throw new KeyStoreStorageException("Failed to save keystore to Mongo collection for node ", ex);
        }
    }
}
