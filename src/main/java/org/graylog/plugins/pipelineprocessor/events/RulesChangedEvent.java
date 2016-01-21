/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
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
