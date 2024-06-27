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

import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.graylog.security.certutil.ca.CAKeyPair;
import org.graylog.security.certutil.ca.PemCaReaderTest;
import org.graylog.security.certutil.ca.exceptions.CACreationException;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog2.Configuration;
import org.graylog2.bootstrap.preflight.web.resources.model.CAType;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.time.Duration;
import java.util.List;

class CaPersistenceServiceTest {

    @Test
    void testCreate() throws KeyStoreStorageException, KeyStoreException, CACreationException {
        final Configuration configuration = Mockito.mock(Configuration.class);
        final String password = RandomStringUtils.randomAlphanumeric(20);
        final EncryptedValueService encryptionService = new EncryptedValueService(password);
        final InMemoryClusterConfigService clusterConfigService = new InMemoryClusterConfigService();
        final CaPersistenceService service = new CaPersistenceService(configuration, password, Mockito.mock(ClusterEventBus.class), clusterConfigService, encryptionService);

        service.create("my-org", 30);

        Assertions.assertThat(service.loadKeyStore())
                .isPresent()
                .hasValueSatisfying(k -> {
                    Assertions.assertThat(k.password()).isEqualTo(password);
                    try {
                        Assertions.assertThat(k.keyStore().getCertificate("ca")).isNotNull();
                    } catch (KeyStoreException e) {
                        throw new RuntimeException(e);
                    }
                });

        service.startOver();

        Assertions.assertThat(service.loadKeyStore()).isEmpty();
    }

    @Test
    void testConfiguredFS(@TempDir Path tempDir) throws Exception {
        final Configuration configuration = Mockito.spy(new Configuration());

        final String existingCaPassword = RandomStringUtils.randomAlphanumeric(20);
        KeyStore keyStore = CAKeyPair.create("my-org", existingCaPassword.toCharArray(), Duration.ofDays(31)).toKeyStore();
        final Path caPath = tempDir.resolve("my-ca-keystore.jks");
        try (FileOutputStream store = new FileOutputStream(caPath.toFile())) {
            keyStore.store(store, existingCaPassword.toCharArray());
        }

        Mockito.when(configuration.getCaKeystoreFile()).thenReturn(caPath);
        Mockito.when(configuration.getCaPassword()).thenReturn(existingCaPassword);


        final String password = RandomStringUtils.randomAlphanumeric(20);
        final EncryptedValueService encryptionService = new EncryptedValueService(password);
        final InMemoryClusterConfigService clusterConfigService = new InMemoryClusterConfigService();
        final CaPersistenceService service = new CaPersistenceService(configuration, password, Mockito.mock(ClusterEventBus.class), clusterConfigService, encryptionService);
        Assertions.assertThat(service.get())
                .isPresent()
                .hasValueSatisfying(info -> {
                    Assertions.assertThat(info.id()).isEqualTo("local CA");
                    Assertions.assertThat(info.type()).isEqualTo(CAType.LOCAL);
                });


        Assertions.assertThat(service.loadKeyStore())
                .isPresent()
                .hasValueSatisfying(k -> {
                    Assertions.assertThat(k.password()).isEqualTo(existingCaPassword);
                    try {
                        Assertions.assertThat(k.keyStore().getCertificate("ca")).isNotNull();
                    } catch (KeyStoreException e) {
                        throw new RuntimeException(e);
                    }
                });
    }


    @Test
    void testUpload() throws Exception {
        final Configuration configuration = Mockito.spy(new Configuration());

        final String password = RandomStringUtils.randomAlphanumeric(20);
        final EncryptedValueService encryptionService = new EncryptedValueService(password);
        final InMemoryClusterConfigService clusterConfigService = new InMemoryClusterConfigService();
        final CaPersistenceService service = new CaPersistenceService(configuration, password, Mockito.mock(ClusterEventBus.class), clusterConfigService, encryptionService);
        final String existingCaPassword = "foobar";

        // TODO: it would be nice to generate the PemCaReaderTest.PEM_CERT_WITH_ENCRYPTED_KEY here instead of using hardcoded
        // values. But so far haven't found any way to PEM-encode private key correctly, so the upload accepts it.
        service.upload(existingCaPassword, List.of(
                getBodyPart(PemCaReaderTest.PEM_CERT_WITH_ENCRYPTED_KEY)
        ));

        Assertions.assertThat(service.get())
                .isPresent()
                .hasValueSatisfying(info -> {
                    Assertions.assertThat(info.id()).isEqualTo("GRAYLOG CA");
                    Assertions.assertThat(info.type()).isEqualTo(CAType.GENERATED);
                });


        Assertions.assertThat(service.loadKeyStore())
                .isPresent()
                .hasValueSatisfying(k -> {
                    Assertions.assertThat(k.password()).isEqualTo(password);
                    try {
                        Assertions.assertThat(k.keyStore().getCertificate("ca")).isNotNull();
                    } catch (KeyStoreException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private FormDataBodyPart getBodyPart(String encodedPK) {
        final FormDataBodyPart part = Mockito.mock(FormDataBodyPart.class);
        Mockito.when(part.getEntityAs(Mockito.eq(InputStream.class))).thenReturn(new ByteArrayInputStream(encodedPK.getBytes(StandardCharsets.UTF_8)));
        return part;
    }
}
