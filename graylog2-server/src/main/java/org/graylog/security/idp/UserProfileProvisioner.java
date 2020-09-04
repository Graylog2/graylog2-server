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

import com.google.auto.value.AutoValue;
import de.huxhorn.sulky.ulid.ULID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class UserProfileProvisioner {
    private static final Logger LOG = LoggerFactory.getLogger(UserProfileProvisioner.class);
    private final ULID ulid;

    @Inject
    public UserProfileProvisioner(ULID ulid) {
        this.ulid = ulid;
    }

    public UserProfile provision(Details profileDetails) {
        // TODO: Add real implementation to create or update the user profile based on the given details

        final UserProfile userProfile = UserProfile.builder()
                .uid(ulid.nextULID()) // TODO: Don't use new ID when profile already exists!
                .idpBackend(profileDetails.idpBackend())
                .idpGuid(profileDetails.idpGuid())
                .username(profileDetails.username())
                .email(profileDetails.email())
                .fullName(profileDetails.fullName())
                .build();

        LOG.info("Provisioning user profile: {}", userProfile);

        return userProfile;
    }

    public Details.Builder newDetails() {
        return Details.builder();
    }

    @AutoValue
    public static abstract class Details {
        public abstract String username();

        public abstract String email();

        public abstract String fullName();

        public abstract String idpBackend();

        public abstract String idpGuid();

        public static Builder builder() {
            return new AutoValue_UserProfileProvisioner_Details.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder username(String username);

            public abstract Builder email(String email);

            public abstract Builder fullName(String fullName);

            public abstract Builder idpBackend(String idpBackend);

            public abstract Builder idpGuid(String idpGuid);

            public abstract Details build();
        }
    }
}
