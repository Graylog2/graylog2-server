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
package org.graylog.security.certutil.cert.storage;

import org.graylog.security.certutil.CertConstants;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.cluster.preflight.DataNodeProvisioningConfig;
import org.graylog2.cluster.preflight.DataNodeProvisioningService;
import org.graylog2.cluster.preflight.DataNodeProvisioningServiceImpl;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class CertChainMongoStorageTest {

    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void testChainStorageSaveAndRetrieve() throws Exception {
        final String nodeId = "test-node-id";
        final DataNodeProvisioningService mongoService = new DataNodeProvisioningServiceImpl(
                new MongoJackObjectMapperProvider(new ObjectMapperProvider().get()),
                mongodb.mongoConnection()
        );
        CertChainMongoStorage toTest = new CertChainMongoStorage(mongoService);
        mongoService.save(DataNodeProvisioningConfig.builder().nodeId(nodeId).state(DataNodeProvisioningConfig.State.UNCONFIGURED).build());

        KeyStore testKeystore = KeyStore.getInstance(CertConstants.PKCS12);
        testKeystore.load(new FileInputStream("src/test/resources/org/graylog/security/certutil/keystore/storage/sample_certificate_keystore.p12"), "password".toCharArray());
        final Certificate[] testFileCertChain = testKeystore.getCertificateChain("datanode");
        CertificateChain certificateChain = new CertificateChain((X509Certificate) testFileCertChain[0],
                Arrays.stream(testFileCertChain)
                        .skip(1)
                        .map(c -> (X509Certificate) c)
                        .collect(Collectors.toList()));

        toTest.writeCertChain(certificateChain, nodeId);
        final Optional<CertificateChain> retrievedCertChain = toTest.readCertChain(nodeId);
        assertThat(retrievedCertChain)
                .isPresent()
                .contains(certificateChain);

        assertThat(retrievedCertChain.get().signedCertificate())
                .satisfies(sc -> assertEquals("CN=localhost", sc.getSubjectX500Principal().getName()));

    }

}
