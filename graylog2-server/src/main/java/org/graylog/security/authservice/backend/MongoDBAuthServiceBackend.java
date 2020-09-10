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

import org.graylog.security.authservice.AuthServiceBackend;
import org.graylog.security.authservice.AuthServiceCredentials;
import org.graylog.security.authservice.UserProfile;
import org.graylog.security.authservice.UserProfileProvisioner;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.security.PasswordAlgorithm;
import org.graylog2.security.PasswordAlgorithmFactory;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

public class MongoDBAuthServiceBackend implements AuthServiceBackend {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDBAuthServiceBackend.class);

    private final UserService userService;
    private final PasswordAlgorithmFactory passwordAlgorithmFactory;

    @Inject
    public MongoDBAuthServiceBackend(UserService userService, PasswordAlgorithmFactory passwordAlgorithmFactory) {
        this.userService = userService;
        this.passwordAlgorithmFactory = passwordAlgorithmFactory;
    }

    @Override
    public Optional<UserProfile> authenticateAndProvision(AuthServiceCredentials authCredentials,
                                                          UserProfileProvisioner userProfileProvisioner) {
        final String username = authCredentials.username();

        LOG.info("Trying to load user <{}> from database", username);
        final User user = userService.load(username);
        if (user == null) {
            LOG.warn("User <{}> not found in database", username);
            return Optional.empty();
        }
        if (user.isLocalAdmin()) {
            throw new IllegalStateException("Local admin user should have been handled earlier and not reach the authentication service authenticator");
        }

        if (!isValidPassword(user, authCredentials.password())) {
            LOG.warn("Failed to validate password for user <{}>", username);
            return Optional.empty();
        }

        LOG.info("Successfully validated password for user <{}>", username);

        final UserProfile userProfile = userProfileProvisioner.provision(userProfileProvisioner.newDetails()
                .username(user.getName())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .authServiceBackend(backendId())
                .authServiceGuid(user.getId())
                .build());

        return Optional.of(userProfile);
    }

    private boolean isValidPassword(User user, String password) {
        final PasswordAlgorithm passwordAlgorithm = passwordAlgorithmFactory.forPassword(user.getHashedPassword());
        if (passwordAlgorithm == null) {
            return false;
        }
        return passwordAlgorithm.matches(user.getHashedPassword(), password);
    }

    @Override
    public String backendId() {
        return "000000000000000000000001";
    }

    @Override
    public String backendTitle() {
        return "Internal MongoDB";
    }
}
