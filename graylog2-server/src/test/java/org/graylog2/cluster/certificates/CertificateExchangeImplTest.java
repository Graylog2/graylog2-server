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
package org.graylog2.cluster.certificates;

import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.graylog.security.certutil.CertConstants;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.csr.CsrGenerator;
import org.graylog.security.certutil.csr.CsrSigner;
import org.graylog.security.certutil.csr.InMemoryKeystoreInformation;
import org.graylog.security.certutil.csr.exceptions.CSRGenerationException;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CertificateExchangeImplTest {

    public static final String FIRST_NODE_ID = "5ca1ab1e-0000-4000-a000-000000000000";
    public static final String SECOND_NODE_ID = "5ca1ab1e-0000-4000-a000-111111111111";
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();
    private CertificateExchange certificateExchange;

    private final String encryptionPassword = RandomStringUtils.randomAlphabetic(20);
    private final char[] keystorePassword = RandomStringUtils.randomAlphabetic(20).toCharArray();
    private KeyStore keystore;

    private KeyPair ca;

    @Before
    public void setUp() throws Exception {
        EncryptedValueService encryptedValueService = new EncryptedValueService(encryptionPassword);
        certificateExchange = new CertificateExchangeImpl(mongodb.mongoConnection(), encryptedValueService);
        this.keystore = generateNodeKeypair().toKeystore(CertConstants.DATANODE_KEY_ALIAS, keystorePassword);
        this.ca = generateCa();
    }

    @Test
    public void testLifecycle() throws Exception {
        // create and persist one CSR
        certificateExchange.requestCertificate(new CertificateSigningRequest(FIRST_NODE_ID, createCertificateSigningRequest("my-datanode-host")));
        certificateExchange.requestCertificate(new CertificateSigningRequest(SECOND_NODE_ID, createCertificateSigningRequest("my-second-node")));

        // trigger all certificates processing. Every CSR will be replaced by a certificate chain
        certificateExchange.signPendingCertificateRequests(this::signCertificate);

        // now let's verify that we have actually replaced the CSR with a certificate and we'll be able to poll one cert chain
        AtomicReference<CertificateChain> firstNodeChainRef = new AtomicReference<>();
        certificateExchange.pollCertificate(FIRST_NODE_ID, firstNodeChainRef::set);
        verifyCertChain(firstNodeChainRef, "my-datanode-host");

        AtomicReference<CertificateChain> secondNodeChainRef = new AtomicReference<>();
        certificateExchange.pollCertificate(SECOND_NODE_ID, secondNodeChainRef::set);
        verifyCertChain(secondNodeChainRef, "my-second-node");
    }

    @Test
    public void testRepeatedCSR() throws CSRGenerationException, IOException {
        certificateExchange.requestCertificate(new CertificateSigningRequest(FIRST_NODE_ID, createCertificateSigningRequest("my-datanode-host")));
        certificateExchange.requestCertificate(new CertificateSigningRequest(FIRST_NODE_ID, createCertificateSigningRequest("my-datanode-host")));
        certificateExchange.requestCertificate(new CertificateSigningRequest(FIRST_NODE_ID, createCertificateSigningRequest("my-datanode-host")));

        AtomicInteger counter = new AtomicInteger();
        certificateExchange.signPendingCertificateRequests(request -> {
            counter.incrementAndGet(); // remember how many certs are we signing
            return signCertificate(request);
        });

        Assertions.assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    public void testFailureDuringChainPolling() throws CSRGenerationException, IOException {
        certificateExchange.requestCertificate(new CertificateSigningRequest(FIRST_NODE_ID, createCertificateSigningRequest("my-datanode-host")));
        certificateExchange.signPendingCertificateRequests(this::signCertificate);

        Assertions.assertThatThrownBy(() ->
                certificateExchange.pollCertificate(FIRST_NODE_ID, certificateChain -> {
            throw new RuntimeException("let's fail, this is expected!");
        })).isInstanceOf(RuntimeException.class);

        AtomicReference<CertificateChain> chain = new AtomicReference<>();
        // second try, the chain should still be there!
        certificateExchange.pollCertificate(FIRST_NODE_ID, chain::set);
        Assertions.assertThat(chain.get()).isNotNull();
    }

    @Test
    public void testFailureDuringSigning() throws CSRGenerationException, IOException {

        certificateExchange.requestCertificate(new CertificateSigningRequest(FIRST_NODE_ID, createCertificateSigningRequest("my-datanode-host")));

        // fail during the initial signing process
        certificateExchange.signPendingCertificateRequests(request -> {
            throw new RuntimeException("let's fail, this is expected!");
        });

        AtomicInteger counter = new AtomicInteger();
        // now test again and check that there is indeed one CSR to process
        certificateExchange.signPendingCertificateRequests(request -> {
            counter.incrementAndGet(); // remember how many certs are we signing
            return signCertificate(request);
        });

        Assertions.assertThat(counter.get()).isEqualTo(1);

    }

    private void verifyCertChain(AtomicReference<CertificateChain> ref, String hostname) {
        Assertions.assertThat(ref.get()).isNotNull();
        final CertificateChain chain = ref.get();
        final X509Certificate cert = chain.signedCertificate();
        Assertions.assertThat(cert.getSubjectX500Principal().toString()).isEqualTo("CN=" + hostname);
        Assertions.assertThat(chain.caCertificates()).isNotEmpty();
    }

    private CertificateChain signCertificate(CertificateSigningRequest csr) {
        try {
            final CsrSigner signer = new CsrSigner();
            final X509Certificate cert = signer.sign(ca.privateKey(), ca.certificate(), csr.request(), 30);
            return new CertificateChain(cert, List.of(ca.certificate()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PKCS10CertificationRequest createCertificateSigningRequest(String hostname) throws CSRGenerationException {
        final InMemoryKeystoreInformation keystore = new InMemoryKeystoreInformation(this.keystore, keystorePassword);
        return CsrGenerator.generateCSR(keystore, CertConstants.DATANODE_KEY_ALIAS, hostname, Collections.emptyList());
    }

    private static KeyPair generateNodeKeypair() throws Exception {
        final CertRequest certRequest = CertRequest.selfSigned(CertConstants.DATANODE_KEY_ALIAS)
                .isCA(false)
                .validity(Duration.ofDays(99 * 365));

        return CertificateGenerator.generate(certRequest);
    }

    private static KeyPair generateCa() throws Exception {
        final CertRequest certRequest = CertRequest.selfSigned(CertConstants.CA_KEY_ALIAS)
                .isCA(true)
                .validity(Duration.ofDays(99 * 365));

        return CertificateGenerator.generate(certRequest);
    }
}
