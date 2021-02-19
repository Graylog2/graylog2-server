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

import com.unboundid.util.Base64;
import org.graylog.security.authservice.AuthServiceBackend;
import org.graylog.security.authservice.AuthServiceBackendDTO;
import org.graylog.security.authservice.AuthServiceCredentials;
import org.graylog.security.authservice.AuthenticationDetails;
import org.graylog.security.authservice.ProvisionerService;
import org.graylog.security.authservice.UserDetails;
import org.graylog.security.authservice.test.AuthServiceBackendTestResult;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.security.PasswordAlgorithm;
import org.graylog2.security.PasswordAlgorithmFactory;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Optional;

public class MongoDBAuthServiceBackend implements AuthServiceBackend {
    public static final String NAME = "internal-mongodb";
    private static final Logger LOG = LoggerFactory.getLogger(MongoDBAuthServiceBackend.class);

    private final UserService userService;
    private final EncryptedValueService encryptedValueService;
    private final PasswordAlgorithmFactory passwordAlgorithmFactory;

    @Inject
    public MongoDBAuthServiceBackend(UserService userService,
                                     EncryptedValueService encryptedValueService,
                                     PasswordAlgorithmFactory passwordAlgorithmFactory) {
        this.userService = userService;
        this.encryptedValueService = encryptedValueService;
        this.passwordAlgorithmFactory = passwordAlgorithmFactory;
    }

    @Override
    public Optional<AuthenticationDetails> authenticateAndProvision(AuthServiceCredentials authCredentials,
                                                          ProvisionerService provisionerService) {
        final String username = authCredentials.username();

        LOG.debug("Trying to load user <{}> from database", username);
        final User user = userService.load(username);
        if (user == null) {
            LOG.debug("User <{}> not found in database", username);
            return Optional.empty();
        }
        if (user.isLocalAdmin()) {
            throw new IllegalStateException("Local admin user should have been handled earlier and not reach the authentication service authenticator");
        }
        if (!user.getAccountStatus().equals(User.AccountStatus.ENABLED)) {
            LOG.warn("Account for user <{}> is disabled.", user.getName());
            return Optional.empty();
        }
        if (user.isExternalUser()) {
            // We don't store passwords for users synced from an authentication service, so we can't handle them here.
            LOG.trace("Skipping mongodb-based password check for external user {}", authCredentials.username());
            return Optional.empty();
        }

        if (!authCredentials.isAuthenticated()) {
            if (!isValidPassword(user, authCredentials.password())) {
                LOG.warn("Failed to validate password for user <{}>", username);
                return Optional.empty();
            }
        }

        LOG.debug("Successfully validated password for user <{}>", username);

        final UserDetails userDetails = provisionerService.provision(provisionerService.newDetails(this)
                .databaseId(user.getId())
                .username(user.getName())
                .accountIsEnabled(user.getAccountStatus().equals(User.AccountStatus.ENABLED))
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                // No need to set default roles because MongoDB users will not be provisioned by the provisioner
                .defaultRoles(Collections.emptySet())
                .base64AuthServiceUid(Base64.encode(user.getId()))
                .build());

        return Optional.of(AuthenticationDetails.builder().userDetails(userDetails).build());
    }

    private boolean isValidPassword(User user, EncryptedValue password) {
        final PasswordAlgorithm passwordAlgorithm = passwordAlgorithmFactory.forPassword(user.getHashedPassword());
        if (passwordAlgorithm == null) {
            return false;
        }
        return passwordAlgorithm.matches(user.getHashedPassword(), encryptedValueService.decrypt(password));
    }

    @Override
    public String backendType() {
        return NAME;
    }

    @Override
    public String backendId() {
        return AuthServiceBackend.INTERNAL_BACKEND_ID;
    }

    @Override
    public String backendTitle() {
        return "Internal MongoDB";
    }

    @Override
    public AuthServiceBackendDTO prepareConfigUpdate(AuthServiceBackendDTO existingBackend, AuthServiceBackendDTO newBackend) {
        return newBackend;
    }

    @Override
    public AuthServiceBackendTestResult testConnection(@Nullable AuthServiceBackendDTO existingBackendConfig) {
        return AuthServiceBackendTestResult.createFailure("Not implemented");
    }

    @Override
    public AuthServiceBackendTestResult testLogin(AuthServiceCredentials credentials, @Nullable AuthServiceBackendDTO existingConfig) {
        return AuthServiceBackendTestResult.createFailure("Not implemented");
    }
}
