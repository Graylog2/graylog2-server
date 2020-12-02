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
package org.graylog.security.authservice.ldap;

import com.google.common.collect.ImmutableList;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import org.assertj.core.api.Assertions;
import org.graylog.testing.ldap.LDAPTestUtils;
import org.graylog.testing.ldap.OpenLDAPContainer;
import org.graylog2.security.DefaultX509TrustManager;
import org.graylog2.security.TrustManagerProvider;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.security.tls.DefaultTLSProtocolProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.net.ssl.TrustManager;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.security.authservice.ldap.LDAPConnectorConfig.LDAPServer.fromUrl;
import static org.graylog.security.authservice.ldap.LDAPTransportSecurity.START_TLS;
import static org.graylog.security.authservice.ldap.LDAPTransportSecurity.TLS;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
public class UnboundLDAPConnectorTestTLSIT {
    private static final int DEFAULT_TIMEOUT = 60 * 1000;

    @Container
    private static final OpenLDAPContainer container = OpenLDAPContainer.createWithTLS();

    private TrustManagerProvider trustManagerProvider;

    private UnboundLDAPConnector ldapConnector;
    private final EncryptedValueService encryptedValueService = new EncryptedValueService("1234567890abcdef");

    @BeforeEach
    void setUp() throws KeyStoreException, NoSuchAlgorithmException {

        this.trustManagerProvider = mock(TrustManagerProvider.class);

        mockTrustManagerWithSystemKeystore();

        this.ldapConnector = new UnboundLDAPConnector(DEFAULT_TIMEOUT, DefaultTLSProtocolProvider.getDefaultSupportedTlsProtocols(), trustManagerProvider, encryptedValueService);
    }

    @Test
    void shouldNotConnectViaTLSToSelfSignedCertIfValidationIsRequested() {
        final LDAPConnectorConfig config = createStartTLSConfig(true);

        assertConnectionFailure(config);
    }

    @Test
    void shouldNotConnectViaTLSToCertWithMismatchingCommonNameIfValidationIsRequested() throws Exception {
        mockTrustManagerWithKeystore(singleCA());

        final LDAPConnectorConfig config = createConfig(ipURI(), START_TLS, true);

        assertConnectionFailure(config);
    }

    @Test
    void shouldConnectViaTLSToTrustedCertWithMatchingCommonNameIfValidationIsRequested() throws Exception {
        mockTrustManagerWithKeystore(singleCA());

        final LDAPConnectorConfig config = createConfig(hostnameURI(), START_TLS, true);

        assertConnectionSuccess(config);
    }

    @Test
    void shouldConnectViaTLSToSelfSignedCertIfValidationIsNotRequested() throws Exception {
        final LDAPConnectorConfig config = createStartTLSConfig(false);

        assertConnectionSuccess(config);
    }

    @Test
    void shouldNotConnectViaSSLToSelfSignedCertIfValidationIsRequested() {
        final LDAPConnectorConfig config = createTLSConfig(true);

        assertConnectionFailure(config);
    }

    @Test
    void shouldNotConnectViaSSLToTrustedCertWithMismatchingHostnameIfValidationIsRequested() throws Exception {
        mockTrustManagerWithKeystore(singleCA());

        final LDAPConnectorConfig config = createConfig(ipTLSURI(), TLS, true);

        assertConnectionFailure(config);
    }

    @Test
    void shouldConnectViaSSLToTrustedCertWithMatchingHostnameIfValidationIsRequested() throws Exception {
        mockTrustManagerWithKeystore(singleCA());

        final LDAPConnectorConfig config = createTLSConfig(true);

        assertConnectionSuccess(config);
    }

    @Test
    void shouldConnectViaTLSToTrustedCertWithMatchingCommonNamesIfValidationIsRequested() throws Exception {
        mockTrustManagerWithKeystore(singleCA());

        LDAPConnectorConfig config = createConfig(hostnameURI(), START_TLS, true);
        List<LDAPConnectorConfig.LDAPServer> servers = ImmutableList.of(fromUrl(dummyHostnameURI().toString()), fromUrl(hostnameURI().toString()));
        config = config.toBuilder().serverList(servers).build();

        assertConnectionSuccess(config);
    }


    @Test
    void shouldConnectViaSSLToSelfSignedCertIfValidationIsNotRequested() throws Exception {
        assertConnectionSuccess(createTLSConfig(false));
    }

    private LDAPConnectorConfig createStartTLSConfig(boolean verifyCertificates) {
        return createConfig(hostnameURI(), START_TLS, verifyCertificates);
    }

    private LDAPConnectorConfig createTLSConfig(boolean verifyCertificates) {
        return createConfig(hostnameTLSURI(), TLS, verifyCertificates);
    }

    private LDAPConnectorConfig createConfig(URI uri, LDAPTransportSecurity transportSecurity, boolean verifyCertificates) {
        return LDAPConnectorConfig.builder()
                .systemUsername(container.bindDn())
                .systemPassword(encryptedValueService.encrypt(container.bindPassword()))
                .serverList(Collections.singletonList(fromUrl(uri.toString())))
                .transportSecurity(transportSecurity)
                .verifyCertificates(verifyCertificates)
                .build();
    }

    private void assertConnectionFailure(LDAPConnectorConfig config) {
        Assertions.assertThatThrownBy(() -> ldapConnector.connect(config)).isNotNull();
    }

    private void assertConnectionSuccess(LDAPConnectorConfig config) throws GeneralSecurityException, LDAPException {
        try (final LDAPConnection connection = ldapConnector.connect(config)) {
            assertThat(connection.getLastBindRequest()).isNotNull();
            assertThat(connection.isConnected()).isTrue();
            if (config.transportSecurity() == START_TLS) {
                assertThat(connection.getStartTLSRequest()).isNotNull();
            }
            assertThat(connection.getSSLSession()).isNotNull();
        }
    }

    private void mockTrustManagerWithSystemKeystore() throws KeyStoreException, NoSuchAlgorithmException {
        mockTrustManagerWithKeystore(null);
    }

    private void mockTrustManagerWithKeystore(KeyStore keyStore) throws KeyStoreException, NoSuchAlgorithmException {
        when(this.trustManagerProvider.create(anyString()))
                .then((invocation) -> provideTrustManager((String) invocation.getArgument(0), keyStore));
        when(this.trustManagerProvider.create(anyList()))
                .then((invocation) -> provideTrustManager((List<String>) invocation.getArgument(0), keyStore));
    }

    private KeyStore singleCA() {
        return LDAPTestUtils.getKeystore("single-ca.jks");
    }

    private TrustManager provideTrustManager(String host, KeyStore keyStore) {
        return provideTrustManager(ImmutableList.of(host), keyStore);
    }

    private TrustManager provideTrustManager(List<String> hosts, KeyStore keyStore) {
        try {
            return new DefaultX509TrustManager(hosts, keyStore);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private URI dummyHostnameURI() {
        return URI.create(String.format(Locale.US, "ldap://0.0.0.0:9"));
    }

    private URI hostnameURI() {
        return URI.create(String.format(Locale.US, "ldap://%s:%d", container.getHost(), container.ldapPort()));
    }

    private URI hostnameTLSURI() {
        return URI.create(String.format(Locale.US, "ldaps://%s:%d", container.getHost(), container.ldapsPort()));
    }

    private URI ipURI() {
        return URI.create(String.format(Locale.US, "ldap://127.0.0.1:%d", container.ldapPort()));
    }

    private URI ipTLSURI() {
        return URI.create(String.format(Locale.US, "ldaps://127.0.0.1:%d", container.ldapsPort()));
    }
}
