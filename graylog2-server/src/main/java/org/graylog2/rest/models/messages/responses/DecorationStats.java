package org.graylog2.rest.models.messages.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@AutoValue
@JsonAutoDetect
public abstract class DecorationStats {
    private static final String FIELD_ADDED_FIELDS = "added_fields";
    private static final String FIELD_CHANGED_FIELDS = "changed_fields";
    private static final String FIELD_REMOVED_FIELDS = "removed_fields";

    @JsonIgnore
    public abstract Map<String, Object> originalMessage();

    @JsonIgnore
    public abstract Map<String, Object> decoratedMessage();

    @JsonProperty(FIELD_ADDED_FIELDS)
    public Map<String, Object> addedFields() {
        return Sets.difference(decoratedMessage().keySet(), originalMessage().keySet())
            .stream()
            .collect(Collectors.toMap(Function.identity(), key -> decoratedMessage().get(key)));
    }

    @JsonProperty(FIELD_CHANGED_FIELDS)
    public Map<String, Object> changedFields() {
        return Sets.intersection(originalMessage().keySet(), decoratedMessage().keySet())
            .stream()
            .filter(key -> !originalMessage().get(key).equals(decoratedMessage().get(key)))
            .collect(Collectors.toMap(Function.identity(), key -> originalMessage().get(key)));
    }

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
