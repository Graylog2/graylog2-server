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
package org.graylog.security.shares;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.grn.GRN;
import org.graylog.security.Capability;
import org.graylog.security.entities.EntityDescriptor;
import org.graylog2.plugin.rest.ValidationResult;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = EntityShareResponse.Builder.class)
public abstract class EntityShareResponse {
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

    @JsonProperty("missing_permissions_on_dependencies")
    public abstract ImmutableMap<GRN, Collection<EntityDescriptor>> missingPermissionsOnDependencies();

    @JsonProperty("validation_result")
    public abstract ValidationResult validationResult();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_EntityShareResponse.Builder()
                    .activeShares(Collections.emptySet())
                    .selectedGranteeCapabilities(Collections.emptyMap())
                    .missingPermissionsOnDependencies(Collections.emptyMap())
                    .validationResult(new ValidationResult());
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

        @JsonProperty("missing_permissions_on_dependencies")
        public abstract Builder missingPermissionsOnDependencies(Map<GRN, Collection<EntityDescriptor>> missingDependencies);

        @JsonProperty("validation_result")
        public abstract Builder validationResult(ValidationResult validationResult);

        public abstract EntityShareResponse build();
    }

    @AutoValue
    public static abstract class AvailableGrantee {
        @JsonProperty("id")
        public abstract GRN grn();

        @JsonProperty("type")
        public abstract String type();

        @JsonProperty("title")
        public abstract String title();

        @JsonCreator
        public static AvailableGrantee create(@JsonProperty("id") GRN grn,
                                              @JsonProperty("type") String type,
                                              @JsonProperty("title") String title) {
            return new AutoValue_EntityShareResponse_AvailableGrantee(grn, type, title);
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
            return new AutoValue_EntityShareResponse_AvailableCapability(id, title);
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
            return new AutoValue_EntityShareResponse_ActiveShare(grant, grantee, capability);
        }
    }
}
