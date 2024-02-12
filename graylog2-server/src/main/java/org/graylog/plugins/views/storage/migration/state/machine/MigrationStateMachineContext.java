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
package org.graylog.plugins.views.storage.migration.state.machine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.HashMap;
import java.util.Map;

@AutoValue
public abstract class MigrationStateMachineContext {

    @JsonProperty("action_arguments")
    public abstract Map<MigrationStep, Object> actionArguments();

    public abstract Builder toBuilder();

    public MigrationStateMachineContext withArguments(MigrationStep step, Object args) {
        Map<MigrationStep, Object> currentArgs = actionArguments();
        currentArgs.put(step, args);
        return toBuilder().actionArguments(currentArgs).build();
    }

    @JsonCreator
    public static MigrationStateMachineContext create() {
        return builder()
                .actionArguments(new HashMap<>())
                .build();
    }

    public static Builder builder() {
        return new AutoValue_MigrationStateMachineContext.Builder();
    }


    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty("action_arguments")
        public abstract Builder actionArguments(Map<MigrationStep, Object> actionArguments);

        public abstract MigrationStateMachineContext build();
    }
}
