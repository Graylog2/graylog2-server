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
package org.graylog.security.authservice.ldap;

import com.google.common.collect.ImmutableSet;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import org.assertj.core.api.Assertions;
import org.graylog.testing.ldap.LDAPTestUtils;
import org.graylog2.security.DefaultX509TrustManager;
import org.graylog2.security.TrustManagerProvider;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.net.ssl.TrustManager;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.security.authservice.ldap.LDAPTransportSecurity.START_TLS;
import static org.graylog.security.authservice.ldap.LDAPTransportSecurity.TLS;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
public class UnboundLDAPConnectorTestTLSIT {
    private static final int DEFAULT_TIMEOUT = 60 * 1000;
    private static final Set<String> ENABLED_TLS_PROTOCOLS = ImmutableSet.of("TLSv1.2");
    private static final Integer PORT = 389;
    private static final Integer TLS_PORT = 636;
    private static final String ADMIN_NAME = "cn=admin,dc=example,dc=org";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String CONTAINER_CERTS_PATH = "/container/service/slapd/assets/certs";

    private static final GenericContainer<?> container = new GenericContainer<>("osixia/openldap:1.4.0")
            .waitingFor(Wait.forLogMessage(".*slapd starting.*", 1))
            .withEnv("LDAP_TLS_VERIFY_CLIENT", "allow")
            .withEnv("LDAP_TLS_CRT_FILENAME", "server-cert.pem")
            .withEnv("LDAP_TLS_KEY_FILENAME", "server-key.pem")
            .withEnv("LDAP_TLS_CA_CRT_FILENAME", "CA-cert.pem")
            .withEnv("LDAP_TLS_DH_PARAM_FILENAME", "dhparam.pem")
            .withFileSystemBind(LDAPTestUtils.testTLSCertsPath(), CONTAINER_CERTS_PATH, BindMode.READ_ONLY)
            .withCommand("--copy-service")
            .withNetworkAliases("ldap-server")
            .withStartupTimeout(Duration.ofMinutes(1));

    private TrustManagerProvider trustManagerProvider;

    private UnboundLDAPConnector ldapConnector;
    private final EncryptedValueService encryptedValueService = new EncryptedValueService("1234567890abcdef");

    @BeforeAll
    static void beforeAll() {
        container.start();
    }

    @BeforeEach
    void setUp() throws KeyStoreException, NoSuchAlgorithmException {

        this.trustManagerProvider = mock(TrustManagerProvider.class);

        mockTrustManagerWithSystemKeystore();

        this.ldapConnector = new UnboundLDAPConnector(DEFAULT_TIMEOUT, ENABLED_TLS_PROTOCOLS, trustManagerProvider, encryptedValueService);
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
                .systemUsername(ADMIN_NAME)
                .systemPassword(encryptedValueService.encrypt(ADMIN_PASSWORD))
                .serverList(Collections.singletonList(LDAPConnectorConfig.LDAPServer.fromUrl(uri.toString())))
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

    private URI hostnameURI() {
        return URI.create(String.format(Locale.US, "ldap://%s:%d",
                container.getHost(),
                container.getMappedPort(PORT)));
    }

    private URI hostnameTLSURI() {
        return URI.create(String.format(Locale.US, "ldaps://%s:%d",
                container.getHost(),
                container.getMappedPort(TLS_PORT)));
    }

    private URI ipURI() {
        return URI.create(String.format(Locale.US, "ldap://127.0.0.1:%d",
                container.getMappedPort(PORT)));
    }

    private URI ipTLSURI() {
        return URI.create(String.format(Locale.US, "ldaps://127.0.0.1:%d",
                container.getMappedPort(TLS_PORT)));
    }
}
