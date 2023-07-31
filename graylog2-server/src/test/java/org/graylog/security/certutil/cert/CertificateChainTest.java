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
package org.graylog.security.certutil.cert;

import org.junit.jupiter.api.Test;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CertificateChainTest {

    @Test
    void testChainCreationWithoutCAs() {
        X509Certificate signedCertMock = mock(X509Certificate.class);
        CertificateChain chain = new CertificateChain(signedCertMock, List.of());
        final Certificate[] certificates = chain.toCertificateChainArray();

        assertThat(certificates)
                .hasSize(1)
                .contains(signedCertMock);
    }

    @Test
    void testChainCreationWithNullCAs() {
        X509Certificate signedCertMock = mock(X509Certificate.class);
        CertificateChain chain = new CertificateChain(signedCertMock, null);
        final Certificate[] certificates = chain.toCertificateChainArray();

        assertThat(certificates)
                .hasSize(1)
                .contains(signedCertMock);
    }

    @Test
    void testChainCreationWithMultpileCAs() {
        X509Certificate signedCertMock = mock(X509Certificate.class);
        X509Certificate intermediateCAMock = mock(X509Certificate.class);
        X509Certificate rootCAMock = mock(X509Certificate.class);
        CertificateChain chain = new CertificateChain(signedCertMock, List.of(intermediateCAMock, rootCAMock));
        final Certificate[] certificates = chain.toCertificateChainArray();

        assertThat(certificates)
                .hasSize(3)
                .containsExactly(signedCertMock, intermediateCAMock, rootCAMock);
    }
}
