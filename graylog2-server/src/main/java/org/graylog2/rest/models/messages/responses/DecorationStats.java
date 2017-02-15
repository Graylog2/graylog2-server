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
package org.graylog2.rest.models.messages.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Sets;
import org.graylog.autovalue.WithBeanGetter;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class DecorationStats {
    private static final String FIELD_ADDED_FIELDS = "added_fields";
    private static final String FIELD_CHANGED_FIELDS = "changed_fields";
    private static final String FIELD_REMOVED_FIELDS = "removed_fields";

    @JsonIgnore
    public abstract Map<String, Object> originalMessage();

    @JsonIgnore
    public abstract Map<String, Object> decoratedMessage();

    @SuppressWarnings("unused")
    @JsonProperty(FIELD_ADDED_FIELDS)
    public Map<String, Object> addedFields() {
        return Sets.difference(decoratedMessage().keySet(), originalMessage().keySet())
            .stream()
            .collect(Collectors.toMap(Function.identity(), key -> decoratedMessage().get(key)));
    }

    @SuppressWarnings("unused")
    @JsonProperty(FIELD_CHANGED_FIELDS)
    public Map<String, Object> changedFields() {
        return Sets.intersection(originalMessage().keySet(), decoratedMessage().keySet())
            .stream()
            .filter(key -> !originalMessage().get(key).equals(decoratedMessage().get(key)))
            .collect(Collectors.toMap(Function.identity(), key -> originalMessage().get(key)));
    }

    @SuppressWarnings("unused")
    @JsonProperty(FIELD_REMOVED_FIELDS)
    public Map<String, Object> removedFields() {
        return Sets.difference(originalMessage().keySet(), decoratedMessage().keySet())
            .stream()
            .collect(Collectors.toMap(Function.identity(), key -> originalMessage().get(key)));
    }

    public static DecorationStats create(Map<String, Object> originalMessage,
                                         Map<String, Object> decoratedMessage) {
        return new AutoValue_DecorationStats(originalMessage, decoratedMessage);
    }
}
