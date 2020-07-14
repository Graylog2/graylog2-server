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

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOperationException;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.assertj.core.api.Assertions;
import org.graylog2.rest.models.system.ldap.requests.LdapTestConfigRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Testcontainers
public class LdapConnectorSSLTLSIT {
    private static final String NETWORK_ALIAS = "ldapserver";
    private static final Integer PORT = 389;
    private static final Integer SSL_PORT = 636;

    @Container
    private final GenericContainer topLevelContainer = new GenericContainer("rroemhild/test-openldap:latest")
            .waitingFor(Wait.forLogMessage(".*slapd starting.*", 1))
            .withNetworkAliases(NETWORK_ALIAS)
            .withExposedPorts(PORT, SSL_PORT);

    private URI internalUri() {
        return URI.create(String.format(Locale.US, "ldap://%s:%d",
                topLevelContainer.getContainerIpAddress(),
                topLevelContainer.getMappedPort(PORT)));
    }

    private URI internalSSLUri() {
        return URI.create(String.format(Locale.US, "ldaps://%s:%d",
                topLevelContainer.getContainerIpAddress(),
                topLevelContainer.getMappedPort(SSL_PORT)));
    }

    @Test
    void shouldNotConnectViaTLSToSelfSignedCertIfValidationIsRequested() throws Exception {
        final LdapSettingsService ldapSettingsService = mock(LdapSettingsService.class);
        final LdapConnector ldapConnector = new LdapConnector(60, ldapSettingsService);

        final LdapTestConfigRequest request = createTLSTestRequest(false);

        Assertions.assertThatThrownBy(() -> ldapConnector.connect(request))
                .isInstanceOf(LdapException.class)
                .hasRootCauseInstanceOf(LdapOperationException.class)
                .hasMessage("Failed to initialize the SSL context");
    }

    @Test
    void shouldConnectViaTLSToSelfSignedCertIfValidationIsNotRequested() throws Exception {
        final LdapSettingsService ldapSettingsService = mock(LdapSettingsService.class);
        final LdapConnector ldapConnector = new LdapConnector(60, ldapSettingsService);

        final LdapTestConfigRequest request = createTLSTestRequest(true);

        final LdapNetworkConnection connection = ldapConnector.connect(request);

        assertThat(connection.isAuthenticated()).isTrue();
        assertThat(connection.isConnected()).isTrue();
        assertThat(connection.isSecured()).isTrue();
    }

    @NotNull
    private LdapTestConfigRequest createTLSTestRequest(boolean trustAllCertificates) {
        return LdapTestConfigRequest.create(
                "cn=admin,dc=planetexpress,dc=com",
                "GoodNewsEveryone",
                internalUri(),
                true,
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

    @Test
    void shouldNotConnectViaSSLToSelfSignedCertIfValidationIsRequested() throws Exception {
        final LdapSettingsService ldapSettingsService = mock(LdapSettingsService.class);
        final LdapConnector ldapConnector = new LdapConnector(60, ldapSettingsService);

        final LdapTestConfigRequest request = createSSLTestRequest(false);

        Assertions.assertThatThrownBy(() -> ldapConnector.connect(request))
                .satisfies(e -> {
                    assertThat(e).isNotNull();
                })
                .isInstanceOf(LdapException.class)
                .hasRootCauseInstanceOf(LdapOperationException.class)
                .hasMessage("Failed to initialize the SSL context");
    }

    @Test
    void shouldConnectViaSSLToSelfSignedCertIfValidationIsNotRequested() throws Exception {
        final LdapSettingsService ldapSettingsService = mock(LdapSettingsService.class);
        final LdapConnector ldapConnector = new LdapConnector(60, ldapSettingsService);

        final LdapTestConfigRequest request = createSSLTestRequest(true);

        final LdapNetworkConnection connection = ldapConnector.connect(request);

        assertThat(connection.isAuthenticated()).isTrue();
        assertThat(connection.isConnected()).isTrue();
        assertThat(connection.isSecured()).isTrue();
    }

    @NotNull
    private LdapTestConfigRequest createSSLTestRequest(boolean trustAllCertificates) {
        return LdapTestConfigRequest.create(
                "cn=admin,dc=planetexpress,dc=com",
                "GoodNewsEveryone",
                internalSSLUri(),
                false,
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
}
