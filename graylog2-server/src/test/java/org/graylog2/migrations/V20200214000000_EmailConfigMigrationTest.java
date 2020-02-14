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
package org.graylog2.migrations;

import org.graylog2.email.configuration.EmailConfiguration;
import org.graylog2.email.configuration.EmailConfigurationService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class V20200214000000_EmailConfigMigrationTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private V20200214000000_EmailConfigMigration emailConfigMigration;

    @Mock
    private org.graylog2.configuration.EmailConfiguration oldEmailConfiguration;

    @Mock
    private EmailConfigurationService emailConfigurationService;

    @Before
    public void setUp() {
        emailConfigMigration = new V20200214000000_EmailConfigMigration(oldEmailConfiguration, emailConfigurationService);
    }

    @Test
    public void alreadyMigrated() {
        EmailConfiguration emailConfiguration = mock(EmailConfiguration.class);
        when(emailConfigurationService.load()).thenReturn(emailConfiguration);

        emailConfigMigration.upgrade();

        verify(emailConfigurationService, never()).save(any());
    }

    @Test
    public void migrateSuccessful() throws URISyntaxException {
        boolean isEnabled = true;
        String hostname = "random.hostname";
        int port = 25;
        boolean isUseAuth = false;
        boolean isUseTls = false;
        boolean isUseSsl = true;
        String username = "imausername";
        String password = "SupersecurePassword";
        String fromEmail = "random@emaildotgraylog.org";
        URI webInterfaceUri = new URI("https://fake.webmail");

        when(emailConfigurationService.load()).thenReturn(null);
        when(oldEmailConfiguration.isEnabled()).thenReturn(isEnabled);
        when(oldEmailConfiguration.getHostname()).thenReturn(hostname);
        when(oldEmailConfiguration.getPort()).thenReturn(port);
        when(oldEmailConfiguration.isUseAuth()).thenReturn(isUseAuth);
        when(oldEmailConfiguration.isUseTls()).thenReturn(isUseTls);
        when(oldEmailConfiguration.isUseSsl()).thenReturn(isUseSsl);
        when(oldEmailConfiguration.getUsername()).thenReturn(username);
        when(oldEmailConfiguration.getPassword()).thenReturn(password);
        when(oldEmailConfiguration.getFromEmail()).thenReturn(fromEmail);
        when(oldEmailConfiguration.getWebInterfaceUri()).thenReturn(webInterfaceUri);
        ArgumentCaptor<EmailConfiguration> emailConfigurationArgumentCaptor = ArgumentCaptor.forClass(EmailConfiguration.class);

        emailConfigMigration.upgrade();

        verify(emailConfigurationService, times(1)).save(emailConfigurationArgumentCaptor.capture());
        EmailConfiguration migratedEmailConfig = emailConfigurationArgumentCaptor.getValue();
        assertThat(migratedEmailConfig.enabled()).isEqualTo(isEnabled);
        assertThat(migratedEmailConfig.hostname()).isEqualTo(hostname);
        assertThat(migratedEmailConfig.port()).isEqualTo(port);
        assertThat(migratedEmailConfig.useAuth()).isEqualTo(isUseAuth);
        assertThat(migratedEmailConfig.useTls()).isEqualTo(isUseTls);
        assertThat(migratedEmailConfig.useSsl()).isEqualTo(isUseSsl);
        assertThat(migratedEmailConfig.username()).isEqualTo(username);
        assertThat(migratedEmailConfig.password()).isEqualTo(password);
        assertThat(migratedEmailConfig.fromEmail()).isEqualTo(fromEmail);
        assertThat(migratedEmailConfig.webInterfaceUri()).isEqualTo(webInterfaceUri);
    }
}
