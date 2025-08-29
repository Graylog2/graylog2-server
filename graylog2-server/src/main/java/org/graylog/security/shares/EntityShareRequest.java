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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.grn.GRN;
import org.graylog.security.Capability;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class EntityShareRequest {
    public static final EntityShareRequest EMPTY = empty();

    public static final String SELECTED_GRANTEE_CAPABILITIES = "selected_grantee_capabilities";
    public static final String SELECTED_COLLECTIONS = "selected_collections";

    @JsonProperty(SELECTED_GRANTEE_CAPABILITIES)
    public abstract Optional<ImmutableMap<GRN, Capability>> selectedGranteeCapabilities();

    @JsonProperty(SELECTED_COLLECTIONS)
    public abstract Optional<Set<GRN>> selectedCollections();

    public Set<GRN> grantees() {
        return selectedGranteeCapabilities().map(ImmutableMap::keySet).orElse(ImmutableSet.of());
    }

    public Set<Capability> capabilities() {
        return selectedGranteeCapabilities()
                .map(ImmutableMap::values)
                .map(ImmutableSet::copyOf)
                .orElse(ImmutableSet.of());
    }

    public boolean isEmpty() {
        return grantees().isEmpty() && selectedCollections().filter(collections -> !collections.isEmpty()).isEmpty();
    }

    @JsonCreator
    public static EntityShareRequest create(
            @JsonProperty(SELECTED_GRANTEE_CAPABILITIES) @Nullable Map<GRN, Capability> selectedGranteeCapabilities,
            @JsonProperty(SELECTED_COLLECTIONS) @Nullable List<GRN> selectedCollections) {
        final ImmutableMap<GRN, Capability> capabilities = selectedGranteeCapabilities == null ? null : ImmutableMap.copyOf(selectedGranteeCapabilities);
        final ImmutableSet<GRN> collections = selectedCollections == null ? null : ImmutableSet.copyOf(selectedCollections);
        return new AutoValue_EntityShareRequest(Optional.ofNullable(capabilities), Optional.ofNullable(collections));
    }

    public static EntityShareRequest empty() {
        return create(Map.of(), List.of());
    }

    public static EntityShareRequest create(
            Map<GRN, Capability> selectedGranteeCapabilities) {
        return create(selectedGranteeCapabilities, null);
    }
}
