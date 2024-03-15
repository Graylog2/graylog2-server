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
package org.graylog2.indexer.datastream.policy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.graylog2.indexer.datastream.policy.actions.Action;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Policy(@Nullable String policyId,
                     @Nonnull String description,
                     @Nullable String lastUpdatedTime,
                     @Nonnull String defaultState,
                     @Nonnull List<State> states,
                     @Nullable IsmTemplate ismTemplate) {

    public Policy(@Nonnull String description, @Nonnull String defaultState, @Nonnull List<State> states) {
        this(null, description, null, defaultState, states, null);
    }


    public record State(@Nonnull String name, @Nonnull List<Action> actions,
                        @Nonnull List<Transition> transitions) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Transition(@Nonnull String stateName, @Nullable Condition conditions) {}

    public record Condition(String minIndexAge) {}

    /**
     * ISM Template property with index pattern and priority. This must be specified in order for automatically rolled-over
     * data streams indexes to continue to be managed by the policy.
     */
    public record IsmTemplate(List<String> indexPatterns, int priority) {
    }
}
