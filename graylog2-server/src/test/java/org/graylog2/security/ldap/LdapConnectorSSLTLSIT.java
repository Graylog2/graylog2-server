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
import org.apache.directory.api.ldap.model.exception.LdapProtocolErrorException;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.exception.InvalidConnectionException;
import org.assertj.core.api.Assertions;
import org.graylog2.rest.models.system.ldap.requests.LdapTestConfigRequest;
import org.graylog2.security.DefaultX509TrustManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import sun.security.provider.certpath.SunCertPathBuilderException;

import javax.net.ssl.TrustManager;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Duration;
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
    private static final String NETWORK_ALIAS = "ldapserver";
    private static final Integer PORT = 389;
    private static final Integer SSL_PORT = 636;
    public static final String ADMIN_NAME = "cn=admin,dc=example,dc=org";
    public static final String ADMIN_PASSWORD = "admin";
    public static final String CERTS_PATH = "/container/service/slapd/assets/certs";

    private final GenericContainer<?> container = new GenericContainer<>("osixia/openldap:1.4.0")
            .waitingFor(Wait.forLogMessage(".*slapd starting.*", 1))
            .withEnv("LDAP_TLS_VERIFY_CLIENT", "allow")
            .withEnv("LDAP_TLS_CRT_FILENAME", "server-cert.pem")
            .withEnv("LDAP_TLS_KEY_FILENAME", "server-key.pem")
            .withEnv("LDAP_TLS_CA_CRT_FILENAME", "CA-cert.pem")
            .withEnv("LDAP_TLS_DH_PARAM_FILENAME", "dhparam.pem")
            .withFileSystemBind(resource("certs"), CERTS_PATH, BindMode.READ_ONLY)
            .withCommand("--copy-service")
            .withNetworkAliases(NETWORK_ALIAS)
            .withExposedPorts(PORT, SSL_PORT)
            .withStartupTimeout(Duration.ofMinutes(1));

    private LdapConnector.TrustManagerProvider trustManagerProvider;

    private LdapConnector ldapConnector;

    @BeforeEach
    void setUp() throws KeyStoreException, NoSuchAlgorithmException {
        container.start();
        final LdapSettingsService ldapSettingsService = mock(LdapSettingsService.class);
        this.trustManagerProvider = mock(LdapConnector.TrustManagerProvider.class);

        mockTrustManagerWithSystemKeystore();

        this.ldapConnector = new LdapConnector(DEFAULT_TIMEOUT, ENABLED_TLS_PROTOCOLS, ldapSettingsService, trustManagerProvider);
    }

    @Test
    void shouldNotConnectViaTLSToSelfSignedCertIfValidationIsRequested() throws Exception {
        final LdapTestConfigRequest request = createTLSTestRequest(false);

        Assertions.assertThatThrownBy(() -> ldapConnector.connect(request))
                .isInstanceOf(LdapException.class)
                .hasRootCauseInstanceOf(SunCertPathBuilderException.class)
                .hasMessage("SSL handshake failed.");
    }

    @Test
    void shouldNotConnectViaTLSToCertWithMismatchingCommonNameIfValidationIsRequested() throws Exception {
        mockTrustManagerWithKeystore(singleCA());

        final LdapTestConfigRequest request = createRequest(internalUriWithIpAddress(), true, false);

        Assertions.assertThatThrownBy(() -> ldapConnector.connect(request))
                .isInstanceOf(LdapException.class)
                .hasMessage("PROTOCOL_ERROR: The server will disconnect!");
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
    void shouldNotConnectViaSSLToSelfSignedCertIfValidationIsRequested() throws Exception {
        final LdapTestConfigRequest request = createSSLTestRequest(false);

        Assertions.assertThatThrownBy(() -> ldapConnector.connect(request))
                .isInstanceOf(LdapProtocolErrorException.class)
                .hasMessage("PROTOCOL_ERROR: The server will disconnect!");
    }

    @Test
    void shouldNotConnectViaSSLToTrustedCertWithMismatchingHostnameIfValidationIsRequested() throws Exception {
        mockTrustManagerWithKeystore(singleCA());

        final LdapTestConfigRequest request = createRequest(internalSSLUriWithIpAddress(), false, false);

        Assertions.assertThatThrownBy(() -> ldapConnector.connect(request))
                .isInstanceOf(InvalidConnectionException.class)
                .hasMessage("SSL handshake failed.");
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
        return createRequest(internalSSLUri(), true, trustAllCertificates);
    }

    @NotNull
    private LdapTestConfigRequest createSSLTestRequest(boolean trustAllCertificates) {
        return createRequest(internalSSLUri(), false, trustAllCertificates);
    }

    private LdapTestConfigRequest createRequest(URI uri, boolean startTls, boolean trustAllCertificates) {
        return LdapTestConfigRequest.create(
                ADMIN_NAME,
                ADMIN_PASSWORD,
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

    private void assertConnectionSuccess(LdapTestConfigRequest request) throws KeyStoreException, LdapException, NoSuchAlgorithmException {
        final LdapNetworkConnection connection = ldapConnector.connect(request);
        assertThat(connection.isAuthenticated()).isTrue();
        assertThat(connection.isConnected()).isTrue();
        assertThat(connection.isSecured()).isTrue();
    }

    private void mockTrustManagerWithSystemKeystore() throws KeyStoreException, NoSuchAlgorithmException {
        mockTrustManagerWithKeystore(null);
    }

    private void mockTrustManagerWithKeystore(KeyStore keyStore) throws KeyStoreException, NoSuchAlgorithmException {
        when(this.trustManagerProvider.create(anyString()))
                .then((invocation) -> provideTrustManager(invocation.getArgument(0), keyStore));
    }

    private KeyStore singleCA() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(this.getClass().getResource("single-ca.jks").openStream(), "changeit".toCharArray());

        return keystore;
    }

    private TrustManager provideTrustManager(String host, KeyStore keyStore) {
        try {
            return new DefaultX509TrustManager(host, keyStore);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String resource(String path) {
        final URL resourceUrl = this.getClass().getResource(path);
        try {
            final File file = Paths.get(resourceUrl.toURI()).toFile();
            return file.getAbsolutePath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private URI internalUri() {
        return URI.create(String.format(Locale.US, "ldap://%s:%d",
                container.getHost(),
                container.getMappedPort(PORT)));
    }

    private URI internalSSLUri() {
        return URI.create(String.format(Locale.US, "ldaps://%s:%d",
                container.getHost(),
                container.getMappedPort(SSL_PORT)));
    }

    private URI internalSSLUriWithIpAddress() {
        return URI.create(String.format(Locale.US, "ldaps://127.0.0.1:%d",
                container.getMappedPort(SSL_PORT)));
    }

    private URI internalUriWithIpAddress() {
        return URI.create(String.format(Locale.US, "ldap://127.0.0.1:%d",
                container.getMappedPort(SSL_PORT)));
    }
}
