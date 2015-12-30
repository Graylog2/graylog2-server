package org.graylog.plugins.messageprocessor.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
public abstract class RuleSource {

    @JsonProperty("_id")
    @Nullable
    @ObjectId
    public abstract String id();

    @JsonProperty
    public abstract String source();

    @JsonProperty
    public abstract DateTime createdAt();

    @JsonProperty
    public abstract DateTime modifiedAt();

    public static Builder builder() {
        return new AutoValue_RuleSource.Builder();
    }

    public abstract Builder toBuilder();

    @JsonCreator
    public static RuleSource create(@JsonProperty("_id") @ObjectId @Nullable String id,
                                    @JsonProperty("source") String source,
                                    @JsonProperty("created_at") DateTime createdAt,
                                    @JsonProperty("modified_at") DateTime modifiedAt) {
        return builder()
                .id(id)
                .source(source)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .build();
    }

    public RuleSource withId(String savedId) {
        return toBuilder().id(savedId).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract RuleSource build();

        public abstract Builder id(String id);

        public abstract Builder source(String source);

        public abstract Builder createdAt(DateTime createdAt);

        public abstract Builder modifiedAt(DateTime modifiedAt);
    }
}
