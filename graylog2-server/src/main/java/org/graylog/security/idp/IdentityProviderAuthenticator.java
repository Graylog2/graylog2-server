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
package org.graylog.security.idp;

import org.graylog.security.idp.provider.MongoDBIdentityProvider;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;

import javax.inject.Inject;
import java.util.Optional;

public class IdentityProviderAuthenticator {
    private final MongoDBIdentityProvider mongodbProvider;
    private final UserService userService;

    @Inject
    public IdentityProviderAuthenticator(MongoDBIdentityProvider mongodbProvider,
                                         UserService userService) {
        this.mongodbProvider = mongodbProvider;
        this.userService = userService;
    }

    /**
     * Tries to authenticate the username with the given password and returns the authenticated username if successful.
     *
     * @param authCredentials the authentication credentials
     * @return the authenticated username
     */
    public IDPAuthResult authenticate(IDPAuthCredentials authCredentials) {
        final Optional<String> optionalIdPUser = mongodbProvider.authenticate(authCredentials);

        if (optionalIdPUser.isPresent()) {
            // TODO: Load user profile for the returned IdP user instead of User
            final User user = userService.load(optionalIdPUser.get());
            if (user == null) {
                return failResult(authCredentials);
            }

            return IDPAuthResult.builder()
                    .username(authCredentials.username())
                    .userProfileId(user.getName()) // TODO: We need to return the user profile ID here so it will be stored in the session
                    .providerId(mongodbProvider.providerId())
                    .providerTitle(mongodbProvider.providerTitle())
                    .build();
        }

        return failResult(authCredentials);
    }

    private IDPAuthResult failResult(IDPAuthCredentials authCredentials) {
        return IDPAuthResult.failed(authCredentials.username(), mongodbProvider.providerId(), mongodbProvider.providerTitle());
    }
}
