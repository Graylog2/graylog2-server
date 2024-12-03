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
package org.graylog.datanode.opensearch.configuration.beans;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class OpensearchConfigurationPart {
    public abstract List<String> nodeRoles();

    public abstract Map<String, String> keystoreItems();

    public abstract Map<String, String> properties();

    public static Builder builder() {
        return new AutoValue_OpensearchConfigurationPart.Builder()
                .nodeRoles(Collections.emptyList())
                .keystoreItems(Collections.emptyMap())
                .properties(Collections.emptyMap());
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder nodeRoles(List<String> nodeRoles);

        abstract ImmutableList.Builder<String> nodeRolesBuilder();

        public final Builder addNodeRole(String role) {
            nodeRolesBuilder().add(role);
            return this;
        }

        public abstract Builder keystoreItems(Map<String, String> keystoreItems);

        public abstract Builder properties(Map<String, String> properties);

        public abstract OpensearchConfigurationPart build();
    }
}
