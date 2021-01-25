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
package org.graylog.security.authservice.backend;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.assistedinject.Assisted;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import org.graylog.security.authservice.AuthServiceBackend;
import org.graylog.security.authservice.AuthServiceBackendDTO;
import org.graylog.security.authservice.AuthServiceCredentials;
import org.graylog.security.authservice.AuthenticationDetails;
import org.graylog.security.authservice.ProvisionerService;
import org.graylog.security.authservice.UserDetails;
import org.graylog.security.authservice.ldap.LDAPConnectorConfig;
import org.graylog.security.authservice.ldap.LDAPUser;
import org.graylog.security.authservice.ldap.UnboundLDAPConfig;
import org.graylog.security.authservice.ldap.UnboundLDAPConnector;
import org.graylog.security.authservice.test.AuthServiceBackendTestResult;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.shared.security.AuthenticationServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ADAuthServiceBackend implements AuthServiceBackend {
    public static final String TYPE_NAME = "active-directory";

    private static final Logger LOG = LoggerFactory.getLogger(ADAuthServiceBackend.class);

    public static final String AD_OBJECT_GUID = "objectGUID";
    public static final String AD_SAM_ACCOUNT_NAME = "sAMAccountName";
    public static final String AD_USER_PRINCIPAL_NAME = "userPrincipalName";
    public static final String AD_DISPLAY_NAME = "displayName";
    public static final String AD_CN = "cn";

    // Try both to avoid the need to configure the userSearchPattern setting
    public static final Filter AD_DEFAULT_USER_SEARCH_PATTERN = Filter.createANDFilter(
            Filter.createEqualityFilter("objectClass", "user"),
            Filter.createORFilter(
                    Filter.createEqualityFilter(AD_USER_PRINCIPAL_NAME, "{0}"),
                    Filter.createEqualityFilter(AD_SAM_ACCOUNT_NAME, "{0}")
            ));

    public interface Factory extends AuthServiceBackend.Factory<ADAuthServiceBackend> {
        @Override
        ADAuthServiceBackend create(AuthServiceBackendDTO backend);
    }

    private final UnboundLDAPConnector ldapConnector;
    private final AuthServiceBackendDTO backend;
    private final ADAuthServiceBackendConfig config;

    @Inject
    public ADAuthServiceBackend(UnboundLDAPConnector ldapConnector,
                                @Assisted AuthServiceBackendDTO backend) {
        this.ldapConnector = ldapConnector;
        this.backend = backend;
        this.config = (ADAuthServiceBackendConfig) backend.config();
    }

    @Override
    public Optional<AuthenticationDetails> authenticateAndProvision(AuthServiceCredentials authCredentials, ProvisionerService provisionerService) {
        try (final LDAPConnection connection = ldapConnector.connect(config.getLDAPConnectorConfig())) {
            if (connection == null) {
                return Optional.empty();
            }

            final Optional<LDAPUser> optionalUser = findUser(connection, authCredentials);
            if (!optionalUser.isPresent()) {
                LOG.debug("User <{}> not found in Active Directory", authCredentials.username());
                return Optional.empty();
            }

            final LDAPUser userEntry = optionalUser.get();

            if (!userEntry.accountIsEnabled()) {
                LOG.warn("Account disabled within Active Directory for user <{}> (DN: {})", authCredentials.username(), userEntry.dn());
                return Optional.empty();
            }
            if (!authCredentials.isAuthenticated()) {
                if (!isAuthenticated(connection, userEntry, authCredentials)) {
                    LOG.debug("Invalid credentials for user <{}> (DN: {})", authCredentials.username(), userEntry.dn());
                    return Optional.empty();
                }
            }

            final UserDetails userDetails = provisionerService.provision(provisionerService.newDetails(this)
                    .authServiceType(backendType())
                    .authServiceId(backendId())
                    .base64AuthServiceUid(userEntry.base64UniqueId())
                    .username(userEntry.username())
                    .accountIsEnabled(userEntry.accountIsEnabled())
                    .fullName(userEntry.fullName())
                    .email(userEntry.email())
                    .defaultRoles(backend.defaultRoles())
                    .build());

            return Optional.of(AuthenticationDetails.builder().userDetails(userDetails).build());
        } catch (GeneralSecurityException e) {
            LOG.error("Error setting up TLS connection", e);
            throw new AuthenticationServiceUnavailableException("Error setting up TLS connection", e);
        } catch (LDAPException e) {
            LOG.error("ActiveDirectory error", e);
            throw new AuthenticationServiceUnavailableException("ActiveDirectory error", e);
        }
    }

    private boolean isAuthenticated(LDAPConnection connection,
                                    LDAPUser user,
                                    AuthServiceCredentials authCredentials) throws LDAPException {
        return ldapConnector.authenticate(
                connection,
                // We need to bind against AD using the userPrincipalName
                user.entry().nonBlankAttribute(AD_USER_PRINCIPAL_NAME),
                authCredentials.password()
        );
    }

    private Optional<LDAPUser> findUser(LDAPConnection connection, AuthServiceCredentials authCredentials) throws LDAPException {
        final UnboundLDAPConfig searchConfig = UnboundLDAPConfig.builder()
                .userSearchBase(config.userSearchBase())
                .userSearchPattern(config.userSearchPattern())
                .userUniqueIdAttribute(AD_OBJECT_GUID)
                .userNameAttribute(config.userNameAttribute())
                .userFullNameAttribute(config.userFullNameAttribute())
                .build();

        return ldapConnector.searchUserByPrincipal(connection, searchConfig, authCredentials.username());
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
        final ADAuthServiceBackendConfig newConfig = (ADAuthServiceBackendConfig) newBackend.config();

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
            final ADAuthServiceBackendConfig existingConfig = (ADAuthServiceBackendConfig) existingBackend.config();
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
        final ADAuthServiceBackendConfig testConfig = buildTestConfig(existingBackendConfig);

        final LDAPConnectorConfig config = testConfig.getLDAPConnectorConfig();

        if (config.serverList().size() == 1) {
            return testSingleConnection(config, config.serverList().get(0));
        }

        // Test each server separately, so we can see the result for each
        final List<AuthServiceBackendTestResult> testResults = config.serverList().stream().map(server -> testSingleConnection(config, server)).collect(Collectors.toList());

        if (testResults.stream().anyMatch(res -> !res.isSuccess())) {
            return AuthServiceBackendTestResult
                    .createFailure("Test failure",
                            testResults.stream().map(r -> {
                                if (r.isSuccess()) {
                                    return r.message();
                                } else {
                                    return r.message() + " : " + String.join(",", r.errors());
                                }
                            }).collect(Collectors.toList()));
        } else {
            return AuthServiceBackendTestResult.createSuccess("Successfully connected to " + config.serverList());
        }
    }

    private AuthServiceBackendTestResult testSingleConnection(LDAPConnectorConfig config, LDAPConnectorConfig.LDAPServer server) {
        final LDAPConnectorConfig singleServerConfig = config.toBuilder().serverList(ImmutableList.of(server)).build();

        try (final LDAPConnection connection = ldapConnector.connect(singleServerConfig)) {
            if (connection == null) {
                return AuthServiceBackendTestResult.createFailure("Couldn't establish connection to " + server);
            }
            return AuthServiceBackendTestResult.createSuccess("Successfully connected to " + server);
        } catch (Exception e) {
            return AuthServiceBackendTestResult.createFailure(
                    "Couldn't establish connection to " + server,
                    Collections.singletonList(e.getMessage())
            );
        }
    }

    @Override
    public AuthServiceBackendTestResult testLogin(AuthServiceCredentials credentials, @Nullable AuthServiceBackendDTO existingBackendConfig) {
        final ADAuthServiceBackendConfig testConfig = buildTestConfig(existingBackendConfig);

        try (final LDAPConnection connection = ldapConnector.connect(testConfig.getLDAPConnectorConfig())) {
            if (connection == null) {
                return AuthServiceBackendTestResult.createFailure("Couldn't establish connection to " + testConfig.servers());
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
                        "Successfully logged in <" + credentials.username() + "> into " + testConfig.servers(),
                        createTestResult(testConfig, true, true, user.get())
                );
            }
            return AuthServiceBackendTestResult.createFailure(
                    "Login for user <" + credentials.username() + "> failed",
                    createTestResult(testConfig, true, false, user.get())
            );
        } catch (Exception e) {
            return AuthServiceBackendTestResult.createFailure(
                    "Couldn't test user login on " + testConfig.servers(),
                    Collections.singletonList(e.getMessage())
            );
        }
    }

    private ADAuthServiceBackendConfig buildTestConfig(@Nullable AuthServiceBackendDTO existingBackendConfig) {
        final ADAuthServiceBackendConfig.Builder newConfigBuilder = config.toBuilder();

        // If the existing password should be kept and we got an existing config, use the password of the
        // existing config for the connection check. This is needed to make connection tests of existing backends work
        // because the UI doesn't have access to the existing password.
        if (config.systemUserPassword().isKeepValue() && existingBackendConfig != null) {
            final ADAuthServiceBackendConfig existingConfig = (ADAuthServiceBackendConfig) existingBackendConfig.config();
            newConfigBuilder.systemUserPassword(existingConfig.systemUserPassword());
        }

        return newConfigBuilder.build();
    }

    private Map<String, Object> createTestResult(ADAuthServiceBackendConfig config,
                                                 boolean userExists,
                                                 boolean loginSuccess,
                                                 @Nullable LDAPUser user) {
        final ImmutableMap.Builder<String, Object> userDetails = ImmutableMap.<String, Object>builder()
                .put("user_exists", userExists)
                .put("login_success", loginSuccess);

        if (user != null) {
            userDetails.put("user_details", ImmutableMap.<String, String>builder()
                    .put("dn", user.dn())
                    .put("account_enabled", String.valueOf(user.accountIsEnabled()))
                    .put(AD_OBJECT_GUID, user.base64UniqueId())
                    .put(config.userNameAttribute(), user.username())
                    .put(config.userFullNameAttribute(), user.fullName())
                    .put("email", user.email())
                    .build()
            );
        } else {
            userDetails.put("user_details", ImmutableMap.of());
        }

        return userDetails.build();
    }
}
