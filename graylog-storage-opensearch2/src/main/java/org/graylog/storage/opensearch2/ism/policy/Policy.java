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
package org.graylog.storage.opensearch2.ism.policy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.graylog.storage.opensearch2.ism.policy.actions.Action;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;


/**
 * Model for a ISM Policy as defined in <a href="https://opensearch.org/docs/2.11/im-plugin/ism/policies/">...</a>.
 * This model is not yet complete, but can be adjusted as further actions or parameters become relevant.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Policy(@Nullable String policyId,
                     @Nullable String description,
                     @Nullable String lastUpdatedTime,
                     @Nonnull String defaultState,
                     @Nonnull List<State> states) {

    public record State(@Nonnull String name, @Nonnull List<Action> actions,
                        @Nonnull List<Transition> transitions) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Transition(@Nonnull String stateName, @Nullable Condition conditions) {}

    public record Condition(String minIndexAge) {}

}
