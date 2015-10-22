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
package org.graylog2.security.realm;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.TrustAllX509TrustManager;
import org.graylog2.security.ldap.LdapConnector;
import org.graylog2.security.ldap.LdapSettingsService;
import org.graylog2.security.ldap.LdapEntry;
import org.graylog2.security.ldap.LdapSettings;
import org.graylog2.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;

public class LdapUserAuthenticator extends AuthenticatingRealm {
    private static final Logger LOG = LoggerFactory.getLogger(LdapUserAuthenticator.class);

    private final LdapConnector ldapConnector;

    private final LdapSettingsService ldapSettingsService;
    private final UserService userService;

    @Inject
    public LdapUserAuthenticator(LdapConnector ldapConnector, LdapSettingsService ldapSettingsService, UserService userService) {
        this.ldapConnector = ldapConnector;
        this.userService = userService;
        this.ldapSettingsService = ldapSettingsService;
        setAuthenticationTokenClass(UsernamePasswordToken.class);
        setCredentialsMatcher(new AllowAllCredentialsMatcher());
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authtoken) throws AuthenticationException {
        // safe, we only handle this type
        UsernamePasswordToken token = (UsernamePasswordToken) authtoken;

        final LdapConnectionConfig config = new LdapConnectionConfig();
        final LdapSettings ldapSettings = ldapSettingsService.load();
        if (ldapSettings == null || !ldapSettings.isEnabled()) {
            LOG.trace("LDAP is disabled, skipping");
            return null;
        }
        config.setLdapHost(ldapSettings.getUri().getHost());
        config.setLdapPort(ldapSettings.getUri().getPort());
        config.setUseSsl(ldapSettings.getUri().getScheme().startsWith("ldaps"));
        config.setUseTls(ldapSettings.isUseStartTls());
        if (ldapSettings.isTrustAllCertificates()) {
            config.setTrustManagers(new TrustAllX509TrustManager());
        }
        config.setName(ldapSettings.getSystemUserName());
        config.setCredentials(ldapSettings.getSystemPassword());

        final String principal = (String) token.getPrincipal();
        LdapNetworkConnection connection = null;
        try {
            connection = ldapConnector.connect(config);

            if (null == connection) {
                LOG.error("Couldn't connect to LDAP directory");
                return null;
            }

            final String password = String.valueOf(token.getPassword());

            final LdapEntry userEntry = ldapConnector.search(connection,
                                                             ldapSettings.getSearchBase(),
                                                             ldapSettings.getSearchPattern(),
                                                             ldapSettings.getDisplayNameAttribute(),
                                                             principal,
                                                             ldapSettings.isActiveDirectory(),
                                                             ldapSettings.getGroupSearchBase(),
                                                             ldapSettings.getGroupIdAttribute(),
                                                             ldapSettings.getGroupSearchPattern());
            if (userEntry == null) {
                LOG.debug("User {} not found in LDAP", principal);
                return null;
            }

            // needs to use the DN of the entry, not the parameter for the lookup filter we used to find the entry!
            final boolean authenticated = ldapConnector.authenticate(connection,
                    userEntry.getDn(),
                    password);
            if (!authenticated) {
                LOG.info("Invalid credentials for user {} (DN {})", principal, userEntry.getDn());
                return null;
            }
            // user found and authenticated, sync the user entry with mongodb
            final User user = userService.syncFromLdapEntry(userEntry, ldapSettings, principal);
            if (user == null) {
                // in case there was an error reading, creating or modifying the user in mongodb, we do not authenticate the user.
                LOG.error("Unable to sync LDAP user {} (DN {})", userEntry.getBindPrincipal(), userEntry.getDn());
                return null;
            }
        } catch (LdapException e) {
            LOG.error("LDAP error", e);
            return null;
        } catch (CursorException e) {
            LOG.error("Unable to read LDAP entry", e);
            return null;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (IOException e) {
                    LOG.error("Unable to close LDAP connection", e);
                }
            }
        }
        return new SimpleAccount(principal, null, "ldap realm");
    }

    public boolean isEnabled() {
        return ldapSettingsService.load().isEnabled();
    }
}
