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
package org.graylog.security.shares;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.security.Capability;
import org.graylog2.utilities.GRN;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = EntitySharePrepareResponse.Builder.class)
public abstract class EntitySharePrepareResponse {
    @JsonProperty("entity")
    public abstract String entity();

    @JsonProperty("sharing_user")
    public abstract GRN sharingUser();

    @JsonProperty("available_grantees")
    public abstract ImmutableSet<AvailableGrantee> availableGrantees();

    @JsonProperty("available_capabilities")
    public abstract ImmutableSet<AvailableCapability> availableCapabilities();

    @JsonProperty("active_shares")
    public abstract ImmutableSet<ActiveShare> activeShares();

    @JsonProperty("selected_grantee_capabilities")
    public abstract ImmutableMap<GRN, Capability> selectedGranteeCapabilities();

    @JsonProperty("missing_dependencies")
    public abstract ImmutableMap<GRN, MissingDependency> missingDependencies();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_EntitySharePrepareResponse.Builder()
                    .activeShares(Collections.emptySet())
                    .selectedGranteeCapabilities(Collections.emptyMap())
                    .missingDependencies(Collections.emptyMap());
        }

        @JsonProperty("entity")
        public abstract Builder entity(String entity);

        @JsonProperty("sharing_user")
        public abstract Builder sharingUser(GRN sharingUser);

        @JsonProperty("available_grantees")
        public abstract Builder availableGrantees(Set<AvailableGrantee> availableGrantees);

        @JsonProperty("available_capabilities")
        public abstract Builder availableCapabilities(Set<AvailableCapability> availableCapabilities);

        @JsonProperty("active_shares")
        public abstract Builder activeShares(Set<ActiveShare> activeShares);

        @JsonProperty("selected_grantee_capabilities")
        public abstract Builder selectedGranteeCapabilities(Map<GRN, Capability> selectedGranteeCapabilities);

        @JsonProperty("missing_dependencies")
        public abstract Builder missingDependencies(Map<GRN, MissingDependency> missingDependencies);

        public abstract EntitySharePrepareResponse build();
    }

    @AutoValue
    public static abstract class AvailableGrantee {
        @JsonProperty("id")
        public abstract String id();

        @JsonProperty("type")
        public abstract String type();

        @JsonProperty("title")
        public abstract String title();

        @JsonCreator
        public static AvailableGrantee create(@JsonProperty("id") String id,
                                              @JsonProperty("type") String type,
                                              @JsonProperty("title") String title) {
            return new AutoValue_EntitySharePrepareResponse_AvailableGrantee(id, type, title);
        }
    }

    @AutoValue
    public static abstract class AvailableCapability {
        @JsonProperty("id")
        public abstract String id();

        @JsonProperty("title")
        public abstract String title();

        @JsonCreator
        public static AvailableCapability create(@JsonProperty("id") String id,
                                                 @JsonProperty("title") String title) {
            return new AutoValue_EntitySharePrepareResponse_AvailableCapability(id, title);
        }
    }

    @AutoValue
    public static abstract class ActiveShare {
        @JsonProperty("grant")
        public abstract String grant();

        @JsonProperty("grantee")
        public abstract GRN grantee();

        @JsonProperty("capability")
        public abstract Capability capability();

        @JsonCreator
        public static ActiveShare create(@JsonProperty("grant") String grant,
                                         @JsonProperty("grantee") GRN grantee,
                                         @JsonProperty("capability") Capability capability) {
            return new AutoValue_EntitySharePrepareResponse_ActiveShare(grant, grantee, capability);
        }
    }

    @AutoValue
    public static abstract class MissingDependency {
        @JsonProperty("id")
        public abstract String id();

        @JsonProperty("title")
        public abstract String title();

        @JsonProperty("owners")
        public abstract ImmutableSet<String> owners();

        @JsonCreator
        public static MissingDependency create(@JsonProperty("id") String id,
                                               @JsonProperty("title") String title,
                                               @JsonProperty("owners") ImmutableSet<String> owners) {
            return new AutoValue_EntitySharePrepareResponse_MissingDependency(id, Objects.toString(title, "<no title>"), owners);
        }
    }
}
