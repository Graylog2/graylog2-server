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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
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
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.TrustAllX509TrustManager;
import org.graylog2.security.ldap.LdapConnector;
import org.graylog2.security.ldap.LdapSettingsService;
import org.graylog2.shared.security.AuthenticationServiceUnavailableException;
import org.graylog2.shared.security.ldap.LdapEntry;
import org.graylog2.shared.security.ldap.LdapSettings;
import org.graylog2.shared.users.Role;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.RoleService;
import org.graylog2.users.UserImpl;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;

public class LdapUserAuthenticator extends AuthenticatingRealm {
    private static final Logger LOG = LoggerFactory.getLogger(LdapUserAuthenticator.class);
    public static final String NAME = "legacy-ldap";

    private final LdapConnector ldapConnector;

    private final LdapSettingsService ldapSettingsService;
    private final RoleService roleService;
    private final DateTimeZone rootTimeZone;
    private final UserService userService;

    @Inject
    LdapUserAuthenticator(LdapConnector ldapConnector,
                          LdapSettingsService ldapSettingsService,
                          UserService userService,
                          RoleService roleService,
                          @Named("root_timezone") DateTimeZone rootTimeZone) {
        this.ldapConnector = ldapConnector;
        this.userService = userService;
        this.ldapSettingsService = ldapSettingsService;
        this.roleService = roleService;
        this.rootTimeZone = rootTimeZone;
        setAuthenticationTokenClass(UsernamePasswordToken.class);
        setCredentialsMatcher(new AllowAllCredentialsMatcher());
        setCachingEnabled(false);
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authtoken) throws AuthenticationException {
        // safe, we only handle this type
        final UsernamePasswordToken token = (UsernamePasswordToken) authtoken;

        if (isEnabled() == false) {
            LOG.trace("LDAP is disabled, skipping");
            return null;
        }

        final LdapSettings ldapSettings = ldapSettingsService.load();

        final String principal = (String) token.getPrincipal();
        final char[] tokenPassword = firstNonNull(token.getPassword(), new char[0]);
        final String password = String.valueOf(tokenPassword);
        // do not try to look a token up in LDAP if there is no principal or password
        if (isNullOrEmpty(principal) || isNullOrEmpty(password)) {
            LOG.debug("Principal or password were empty. Not trying to look up a token in LDAP.");
            return null;
        }
        try (final LdapNetworkConnection connection = openLdapConnection(ldapSettings)) {
            if (null == connection) {
                LOG.error("Couldn't connect to LDAP directory");
                return null;
            }
            final LdapEntry userEntry = searchLdapUser(connection, principal, ldapSettings);
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
            final User user = syncFromLdapEntry(userEntry, ldapSettings, principal);
            if (user == null) {
                // in case there was an error reading, creating or modifying the user in mongodb, we do not authenticate the user.
                LOG.error("Unable to sync LDAP user {} (DN {})", userEntry.getBindPrincipal(), userEntry.getDn());
                return null;
            }

            return new SimpleAccount(principal, null, "ldap realm");
        } catch (LdapException e) {
            LOG.error("LDAP error", e);
            throw new AuthenticationServiceUnavailableException("LDAP error", e);
        } catch (CursorException e) {
            LOG.error("Unable to read LDAP entry", e);
            throw new AuthenticationServiceUnavailableException("Unable to read LDAP entry", e);
        } catch (Exception e) {
            LOG.error("Error during LDAP user account sync. Cannot log in user {}", principal, e);
            return null;
        }
    }

    protected LdapNetworkConnection openLdapConnection(LdapSettings ldapSettings) throws LdapException {
        final LdapConnectionConfig config = new LdapConnectionConfig();
        config.setLdapHost(ldapSettings.getUri().getHost());
        config.setLdapPort(ldapSettings.getUri().getPort());
        config.setUseSsl(ldapSettings.getUri().getScheme().startsWith("ldaps"));
        config.setUseTls(ldapSettings.isUseStartTls());
        if (ldapSettings.isTrustAllCertificates()) {
            config.setTrustManagers(new TrustAllX509TrustManager());
        }
        config.setName(ldapSettings.getSystemUserName());
        config.setCredentials(ldapSettings.getSystemPassword());
        return ldapConnector.connect(config);
    }

    protected LdapEntry searchLdapUser(LdapNetworkConnection connection, String principal, LdapSettings ldapSettings) throws LdapException, CursorException {
        return ldapConnector.search(connection,
                ldapSettings.getSearchBase(),
                ldapSettings.getSearchPattern(),
                ldapSettings.getDisplayNameAttribute(),
                principal,
                ldapSettings.isActiveDirectory(),
                ldapSettings.getGroupSearchBase(),
                ldapSettings.getGroupIdAttribute(),
                ldapSettings.getGroupSearchPattern());
    }

    public boolean isEnabled() {
        final LdapSettings ldapSettings = ldapSettingsService.load();
        return ldapSettings != null && ldapSettings.isEnabled();
    }

    @Nullable
    public User syncLdapUser(String principal) {

        if (isEnabled() == false) {
            LOG.trace("LDAP is disabled, skipping");
            return null;
        }

        final LdapSettings ldapSettings = ldapSettingsService.load();

        // do not try to look a token up in LDAP if there is no principal or password
        if (isNullOrEmpty(principal)) {
            LOG.debug("Principal was empty. Not trying to sync user with LDAP.");
            return null;
        }
        try (final LdapNetworkConnection connection = openLdapConnection(ldapSettings)) {
            if (null == connection) {
                LOG.error("Couldn't connect to LDAP directory");
                return null;
            }
            final LdapEntry userEntry = searchLdapUser(connection, principal, ldapSettings);
            if (userEntry == null) {
                LOG.debug("User {} not found in LDAP", principal);
                return null;
            }

            // user found, sync the user entry with mongodb
            final User user = syncFromLdapEntry(userEntry, ldapSettings, principal);
            if (user == null) {
                // in case there was an error reading, creating or modifying the user in mongodb, we do not authenticate the user.
                LOG.error("Unable to sync LDAP user {} (DN {})", userEntry.getBindPrincipal(), userEntry.getDn());
                return null;
            }

            return user;
        } catch (LdapException e) {
            LOG.error("LDAP error", e);
        } catch (CursorException e) {
            LOG.error("Unable to read LDAP entry", e);
        } catch (Exception e) {
            LOG.error("Error during LDAP user account sync. Cannot sync user {}", principal, e);
        }

        return null;
    }

    @Nullable
    @VisibleForTesting
    User syncFromLdapEntry(LdapEntry userEntry, LdapSettings ldapSettings, String username) {
        User user = userService.load(username);

        // create new user object if necessary
        if (user == null) {
            user = userService.create();
        }

        // update user attributes from ldap entry
        updateFromLdap(user, userEntry, ldapSettings, username);

        try {
            userService.save(user);
        } catch (ValidationException e) {
            LOG.error("Cannot save user.", e);
            return null;
        }

        return user;
    }

    private void updateFromLdap(User user, LdapEntry userEntry, LdapSettings ldapSettings, String username) {
        final String displayNameAttribute = ldapSettings.getDisplayNameAttribute();
        final String fullName = firstNonNull(userEntry.get(displayNameAttribute), username);

        user.setName(username);
        user.setFullName(fullName);
        user.setExternal(true);

        if (user.getTimeZone() == null) {
            user.setTimeZone(rootTimeZone);
        }

        final String email = userEntry.getEmail();
        if (isNullOrEmpty(email)) {
            LOG.debug("No email address found for user {} in LDAP. Using {}@localhost", username, username);
            user.setEmail(username + "@localhost");
        } else {
            user.setEmail(email);
        }

        // TODO This is a crude hack until we have a proper way to distinguish LDAP users from normal users
        if (isNullOrEmpty(user.getHashedPassword())) {
            ((UserImpl) user).setHashedPassword("User synced from LDAP.");
        }

        // map ldap groups to user roles, if the mapping is present
        final Set<String> translatedRoleIds = Sets.newHashSet(Sets.union(Sets.newHashSet(ldapSettings.getDefaultGroupId()),
                                                                         ldapSettings.getAdditionalDefaultGroupIds()));
        if (!userEntry.getGroups().isEmpty()) {
            // ldap search returned groups, these always override the ones set on the user
            try {
                final Map<String, Role> roleNameToRole = roleService.loadAllLowercaseNameMap();
                for (String ldapGroupName : userEntry.getGroups()) {
                    final String roleName = ldapSettings.getGroupMapping().get(ldapGroupName);
                    if (roleName == null) {
                        LOG.debug("User {}: No group mapping for ldap group <{}>", username, ldapGroupName);
                        continue;
                    }
                    final Role role = roleNameToRole.get(roleName.toLowerCase(Locale.ENGLISH));
                    if (role != null) {
                        LOG.debug("User {}: Mapping ldap group <{}> to role <{}>",
                                  username,
                                  ldapGroupName,
                                  role.getName());
                        translatedRoleIds.add(role.getId());
                    } else {
                        LOG.warn("User {}: No role found for ldap group <{}>", username, ldapGroupName);
                    }
                }

            } catch (NotFoundException e) {
                LOG.error("Unable to load user roles", e);
            }
        } else if (ldapSettings.getGroupMapping().isEmpty()
                || ldapSettings.getGroupSearchBase().isEmpty()
                || ldapSettings.getGroupSearchPattern().isEmpty()
                || ldapSettings.getGroupIdAttribute().isEmpty()) {
            // no group mapping or configuration set, we'll leave the previously set groups alone on sync
            // when first creating the user these will be empty
            translatedRoleIds.addAll(user.getRoleIds());
        }
        user.setRoleIds(translatedRoleIds);
        // preserve the raw permissions (the ones without the synthetic self-edit permissions or the "*" admin one)
        user.setPermissions(user.getPermissions());
    }

}
