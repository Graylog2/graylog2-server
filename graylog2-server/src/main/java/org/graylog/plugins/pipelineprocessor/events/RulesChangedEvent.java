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
package org.graylog.plugins.pipelineprocessor.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Sets;

import java.util.Set;

import static java.util.Collections.emptySet;

@AutoValue
public abstract class RulesChangedEvent {

    @JsonProperty
    public abstract Set<String> deletedRuleIds();

    @JsonProperty
    public abstract Set<String> updatedRuleIds();

    public static Builder builder() {
        return new AutoValue_RulesChangedEvent.Builder().deletedRuleIds(emptySet()).updatedRuleIds(emptySet());
    }

    public static RulesChangedEvent updatedRuleId(String id) {
        return builder().updatedRuleId(id).build();
    }

    public static RulesChangedEvent deletedRuleId(String id) {
        return builder().deletedRuleId(id).build();
    }

    @JsonCreator
    public static RulesChangedEvent create(@JsonProperty("deleted_rule_ids") Set<String> deletedIds, @JsonProperty("updated_rule_ids") Set<String> updatedIds) {
        return builder().deletedRuleIds(deletedIds).updatedRuleIds(updatedIds).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder deletedRuleIds(Set<String> ids);
        public Builder deletedRuleId(String id) {
            return deletedRuleIds(Sets.newHashSet(id));
        }
        public abstract Builder updatedRuleIds(Set<String> ids);
        public Builder updatedRuleId(String id) {
            return updatedRuleIds(Sets.newHashSet(id));
        }
        public abstract RulesChangedEvent build();
    }
}
