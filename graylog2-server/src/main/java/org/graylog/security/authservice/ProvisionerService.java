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
package org.graylog.security.authservice;

import com.google.auto.value.AutoValue;
import de.huxhorn.sulky.ulid.ULID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;

public class ProvisionerService {
    private static final Logger LOG = LoggerFactory.getLogger(ProvisionerService.class);

    private final ULID ulid;
    private final Set<ProvisionerAction> provisionerActions;

    @Inject
    public ProvisionerService(ULID ulid, Set<ProvisionerAction> provisionerActions) {
        this.ulid = ulid;
        this.provisionerActions = provisionerActions;
    }

    public UserProfile provision(Details provisionDetails) {
        final UserProfile userProfile = UserProfile.builder()
                .uid(ulid.nextULID()) // TODO: Don't use new ID when profile already exists!
                .authServiceId(provisionDetails.authServiceId())
                .authServiceUid(provisionDetails.authServiceUid())
                .username(provisionDetails.username())
                .email(provisionDetails.email())
                .fullName(provisionDetails.fullName())
                .build();

        // TODO: Add real implementation to create or update the user profile based on the given details

        LOG.info("Running {} provisioner actions", provisionerActions.size());
        for (final ProvisionerAction action : provisionerActions) {
            try {
                action.provision(userProfile);
            } catch (Exception e) {
                LOG.error("Error running provisioner action <{}>", action.getClass().getCanonicalName(), e);
                // TODO: Should we fail here or just continue?
            }
        }

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

        public abstract String authServiceType();

        public abstract String authServiceId();

        public abstract String authServiceUid();

        public static Builder builder() {
            return new AutoValue_ProvisionerService_Details.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder username(String username);

            public abstract Builder email(String email);

            public abstract Builder fullName(String fullName);

            public abstract Builder authServiceType(String authServiceType);

            public abstract Builder authServiceId(String authServiceId);

            public abstract Builder authServiceUid(String authServiceUid);

            public abstract Details build();
        }
    }
}
