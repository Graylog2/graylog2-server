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
package org.graylog.security.authservice.backend;

import com.google.common.collect.ImmutableMap;
import com.google.inject.assistedinject.Assisted;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import org.graylog.security.authservice.AuthServiceBackend;
import org.graylog.security.authservice.AuthServiceBackendDTO;
import org.graylog.security.authservice.AuthServiceCredentials;
import org.graylog.security.authservice.ProvisionerService;
import org.graylog.security.authservice.UserDetails;
import org.graylog.security.authservice.ldap.LDAPUser;
import org.graylog.security.authservice.ldap.UnboundLDAPConnector;
import org.graylog.security.authservice.test.AuthServiceBackendTestResult;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.shared.security.ldap.LdapEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class LDAPAuthServiceBackend implements AuthServiceBackend {
    public static final String TYPE_NAME = "ldap";

    private static final Logger LOG = LoggerFactory.getLogger(LDAPAuthServiceBackend.class);
    private final LDAPAuthServiceBackendConfig config;

    public interface Factory extends AuthServiceBackend.Factory<LDAPAuthServiceBackend> {
        @Override
        LDAPAuthServiceBackend create(AuthServiceBackendDTO backend);
    }

    private final UnboundLDAPConnector ldapConnector;
    private final AuthServiceBackendDTO backend;

    @Inject
    public LDAPAuthServiceBackend(UnboundLDAPConnector ldapConnector,
                                  @Assisted AuthServiceBackendDTO backend) {
        this.ldapConnector = ldapConnector;
        this.backend = backend;
        this.config = (LDAPAuthServiceBackendConfig) backend.config();
    }

    @Override
    public Optional<UserDetails> authenticateAndProvision(AuthServiceCredentials authCredentials, ProvisionerService provisionerService) {
        try (final LDAPConnection connection = ldapConnector.connect(config.getLDAPConnectorConfig())) {
            if (connection == null) {
                return Optional.empty();
            }

            final Optional<LDAPUser> optionalUser = findUser(connection, authCredentials);
            if (!optionalUser.isPresent()) {
                LOG.debug("User <{}> not found in LDAP", authCredentials.username());
                return Optional.empty();
            }

            final LDAPUser userEntry = optionalUser.get();

            if (!isAuthenticated(connection, userEntry, authCredentials)) {
                LOG.debug("Invalid credentials for user <{}> (DN: {})", authCredentials.username(), userEntry.dn());
                return Optional.empty();
            }

            final UserDetails userDetails = provisionerService.provision(provisionerService.newDetails(this)
                    .authServiceType(backendType())
                    .authServiceId(backendId())
                    .authServiceUid(userEntry.uniqueId())
                    .username(userEntry.username())
                    .fullName(userEntry.fullName())
                    .email(userEntry.email())
                    .defaultRoles(backend.defaultRoles())
                    .build());

            return Optional.of(userDetails);
        } catch (GeneralSecurityException e) {
            LOG.error("Error setting up TLS connection", e);
            return Optional.empty();
        } catch (LDAPException e) {
            LOG.error("LDAP error", e);
            return Optional.empty();
        }
    }

    private boolean isAuthenticated(LDAPConnection connection,
                                    LDAPUser user,
                                    AuthServiceCredentials authCredentials) throws LDAPException {
        return ldapConnector.authenticate(
                connection,
                user.dn(),
                authCredentials.password()
        );
    }

    private Optional<LDAPUser> findUser(LDAPConnection connection, AuthServiceCredentials authCredentials) throws LDAPException {
        return ldapConnector.searchUser(
                connection,
                config.userSearchBase(),
                config.userSearchPattern(),
                authCredentials.username(),
                config.userUniqueIdAttribute(),
                config.userNameAttribute(),
                config.userFullNameAttribute()
        );
    }

    @Override
    public String backendType() {
        return TYPE_NAME;
    }

    @Override
    public String backendId() {
        return backend.id();
    }

    @Override
    public String backendTitle() {
        return backend.title();
    }

    @Override
    public AuthServiceBackendDTO prepareConfigUpdate(AuthServiceBackendDTO existingBackend, AuthServiceBackendDTO newBackend) {
        final LDAPAuthServiceBackendConfig newConfig = (LDAPAuthServiceBackendConfig) newBackend.config();

        if (newConfig.systemUserPassword().isDeleteValue()) {
            // If the system user password should be deleted, use an unset value
            return newBackend.toBuilder()
                    .config(newConfig.toBuilder()
                            .systemUserPassword(EncryptedValue.createUnset())
                            .build())
                    .build();
        }
        if (newConfig.systemUserPassword().isKeepValue()) {
            // If the system user password should be kept, use the value from the existing config
            final LDAPAuthServiceBackendConfig existingConfig = (LDAPAuthServiceBackendConfig) existingBackend.config();
            return newBackend.toBuilder()
                    .config(newConfig.toBuilder()
                            .systemUserPassword(existingConfig.systemUserPassword())
                            .build())
                    .build();
        }

        return newBackend;
    }

    @Override
    public AuthServiceBackendTestResult testConnection(@Nullable AuthServiceBackendDTO existingBackendConfig) {
        final LDAPAuthServiceBackendConfig testConfig = buildTestConfig(existingBackendConfig);

        try (final LDAPConnection connection = ldapConnector.connect(testConfig.getLDAPConnectorConfig())) {
            if (connection == null) {
                return AuthServiceBackendTestResult.createFailure("Couldn't establish connection to " + testConfig.serverUrls());
            }
            return AuthServiceBackendTestResult.createSuccess("Successfully connected to " + testConfig.serverUrls());
        } catch (Exception e) {
            return AuthServiceBackendTestResult.createFailure(
                    "Couldn't establish connection to " + testConfig.serverUrls(),
                    Collections.singletonList(e.getMessage())
            );
        }
    }

    @Override
    public AuthServiceBackendTestResult testLogin(AuthServiceCredentials credentials, @Nullable AuthServiceBackendDTO existingBackendConfig) {
        final LDAPAuthServiceBackendConfig testConfig = buildTestConfig(existingBackendConfig);

        try (final LDAPConnection connection = ldapConnector.connect(testConfig.getLDAPConnectorConfig())) {
            if (connection == null) {
                return AuthServiceBackendTestResult.createFailure("Couldn't establish connection to " + testConfig.serverUrls());
            }
            final Optional<LDAPUser> user = findUser(connection, credentials);

            if (!user.isPresent()) {
                return AuthServiceBackendTestResult.createFailure(
                        "User <" + credentials.username() + "> doesn't exist",
                        createTestResult(testConfig, false, false, null)
                );
            }

            if (isAuthenticated(connection, user.get(), credentials)) {
                return AuthServiceBackendTestResult.createSuccess(
                        "Successfully logged in <" + credentials.username() + "> into " + testConfig.serverUrls(),
                        createTestResult(testConfig, true, true, user.get())
                );
            }
            return AuthServiceBackendTestResult.createFailure(
                    "Login for user <" + credentials.username() + "> failed",
                    createTestResult(testConfig, true, false, user.get())
            );
        } catch (Exception e) {
            return AuthServiceBackendTestResult.createFailure(
                    "Couldn't test user login on " + testConfig.serverUrls(),
                    Collections.singletonList(e.getMessage())
            );
        }
    }

    private LDAPAuthServiceBackendConfig buildTestConfig(@Nullable AuthServiceBackendDTO existingBackendConfig) {
        final LDAPAuthServiceBackendConfig.Builder newConfigBuilder = config.toBuilder();

        // If there is no password set in the current config and we got an existing config, use the password of the
        // existing config for the connection check. This is needed to make connection tests of existing backends work
        // because the UI doesn't have access to the existing password.
        // TODO: This logic doesn't work if a user wants to update a backend that currently has a password set to a
        //       backend that doesn't need a password.
        if (!config.systemUserPassword().isSet() && existingBackendConfig != null) {
            final LDAPAuthServiceBackendConfig existingConfig = (LDAPAuthServiceBackendConfig) existingBackendConfig.config();
            newConfigBuilder.systemUserPassword(existingConfig.systemUserPassword());
        }

        return newConfigBuilder.build();
    }

    private Map<String, Object> createTestResult(LDAPAuthServiceBackendConfig config,
                                                 boolean userExists,
                                                 boolean loginSuccess,
                                                 @Nullable LDAPUser user) {
        final ImmutableMap.Builder<String, Object> userDetails = ImmutableMap.<String, Object>builder()
                .put("user_exists", userExists)
                .put("login_success", loginSuccess);

        if (user != null) {
            userDetails.put("user_details", ImmutableMap.of(
                    "dn", user.dn(),
                    config.userUniqueIdAttribute(), user.uniqueId(),
                    config.userNameAttribute(), user.username(),
                    config.userFullNameAttribute(), user.fullName(),
                    "email", user.email()
            ));
        } else {
            userDetails.put("user_details", ImmutableMap.of());
        }

        return userDetails.build();
    }
}
