/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.security.ldap;

import com.google.common.collect.ImmutableSet;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.assertj.core.api.Assertions;
import org.graylog.testing.ldap.LDAPTestUtils;
import org.graylog.testing.ldap.OpenLDAPContainer;
import org.graylog2.rest.models.system.ldap.requests.LdapTestConfigRequest;
import org.graylog2.security.DefaultX509TrustManager;
import org.graylog2.security.TrustManagerProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
public class LdapConnectorSSLTLSIT {
    private static final int DEFAULT_TIMEOUT = 60 * 1000;
    private static final Set<String> ENABLED_TLS_PROTOCOLS = ImmutableSet.of("TLSv1.2");

    @Container
    private static final OpenLDAPContainer container = OpenLDAPContainer.createWithTLS();

    private TrustManagerProvider trustManagerProvider;

    private LdapConnector ldapConnector;

    @BeforeEach
    void setUp() throws KeyStoreException, NoSuchAlgorithmException {
        final LdapSettingsService ldapSettingsService = mock(LdapSettingsService.class);
        this.trustManagerProvider = mock(TrustManagerProvider.class);

        mockTrustManagerWithSystemKeystore();

        this.ldapConnector = new LdapConnector(DEFAULT_TIMEOUT, ENABLED_TLS_PROTOCOLS, ldapSettingsService, trustManagerProvider);
    }

    @Test
    void shouldNotConnectViaTLSToSelfSignedCertIfValidationIsRequested() {
        final LdapTestConfigRequest request = createTLSTestRequest(false);

        assertConnectionFailure(request);
    }

    @Test
    void shouldNotConnectViaTLSToCertWithMismatchingCommonNameIfValidationIsRequested() throws Exception {
        mockTrustManagerWithKeystore(singleCA());

        final LdapTestConfigRequest request = createRequest(internalUriWithIpAddress(), true, false);

        assertConnectionFailure(request);
    }

    @Test
    void shouldConnectViaTLSToTrustedCertWithMatchingCommonNameIfValidationIsRequested() throws Exception {
        mockTrustManagerWithKeystore(singleCA());

        final LdapTestConfigRequest request = createRequest(internalUri(), true, false);

        assertConnectionSuccess(request);
    }

    @Test
    void shouldConnectViaTLSToSelfSignedCertIfValidationIsNotRequested() throws Exception {
        final LdapTestConfigRequest request = createTLSTestRequest(true);

        assertConnectionSuccess(request);
    }

    @Test
    void shouldNotConnectViaSSLToSelfSignedCertIfValidationIsRequested() {
        final LdapTestConfigRequest request = createSSLTestRequest(false);

        assertConnectionFailure(request);
    }

    @Test
    void shouldNotConnectViaSSLToTrustedCertWithMismatchingHostnameIfValidationIsRequested() throws Exception {
        mockTrustManagerWithKeystore(singleCA());

        final LdapTestConfigRequest request = createRequest(internalSSLUriWithIpAddress(), false, false);

        assertConnectionFailure(request);
    }

    @Test
    void shouldConnectViaSSLToTrustedCertWithMatchingHostnameIfValidationIsRequested() throws Exception {
        mockTrustManagerWithKeystore(singleCA());

        final LdapTestConfigRequest request = createSSLTestRequest(false);

        assertConnectionSuccess(request);
    }

    @Test
    void shouldConnectViaSSLToSelfSignedCertIfValidationIsNotRequested() throws Exception {
        final LdapTestConfigRequest request = createSSLTestRequest(true);

        assertConnectionSuccess(request);
    }

    @NotNull
    private LdapTestConfigRequest createTLSTestRequest(boolean trustAllCertificates) {
        return createRequest(internalUri(), true, trustAllCertificates);
    }

    @NotNull
    private LdapTestConfigRequest createSSLTestRequest(boolean trustAllCertificates) {
        return createRequest(internalSSLUri(), false, trustAllCertificates);
    }

    private LdapTestConfigRequest createRequest(URI uri, boolean startTls, boolean trustAllCertificates) {
        return LdapTestConfigRequest.create(
                container.bindDn(),
                container.bindPassword(),
                uri,
                startTls,
                trustAllCertificates,
                false,
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                null
        );
    }

    private void assertConnectionFailure(LdapTestConfigRequest request) {
        Assertions.assertThatThrownBy(() -> ldapConnector.connect(request)).isNotNull();
    }

    private void assertConnectionSuccess(LdapTestConfigRequest request) throws KeyStoreException, LdapException, NoSuchAlgorithmException, IOException {
        try (final LdapNetworkConnection connection = ldapConnector.connect(request)) {
            assertThat(connection.isAuthenticated()).isTrue();
            assertThat(connection.isConnected()).isTrue();
            assertThat(connection.isSecured()).isTrue();
        }
    }

    private void mockTrustManagerWithSystemKeystore() throws KeyStoreException, NoSuchAlgorithmException {
        mockTrustManagerWithKeystore(null);
    }

    private void mockTrustManagerWithKeystore(KeyStore keyStore) throws KeyStoreException, NoSuchAlgorithmException {
        when(this.trustManagerProvider.create(anyString()))
                .then((invocation) -> provideTrustManager(invocation.getArgument(0), keyStore));
    }

    private KeyStore singleCA() {
        return LDAPTestUtils.getKeystore("single-ca.jks");
    }

    private TrustManager provideTrustManager(String host, KeyStore keyStore) {
        try {
            return new DefaultX509TrustManager(host, keyStore);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private URI internalUri() {
        return URI.create(String.format(Locale.US, "ldap://%s:%d", container.getHost(), container.ldapPort()));
    }

    private URI internalSSLUri() {
        return URI.create(String.format(Locale.US, "ldaps://%s:%d", container.getHost(), container.ldapsPort()));
    }

    private URI internalSSLUriWithIpAddress() {
        return URI.create(String.format(Locale.US, "ldaps://127.0.0.1:%d", container.ldapsPort()));
    }

    private URI internalUriWithIpAddress() {
        return URI.create(String.format(Locale.US, "ldap://127.0.0.1:%d", container.ldapsPort()));
    }
}
