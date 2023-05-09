package org.graylog.plugins.pipelineprocessor.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.pipelineprocessor.db.RuleBuilder;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
public abstract class RuleBuilderRule {

    @JsonProperty("id")
    @Nullable
    @Id
    @ObjectId
    public abstract String id();

    @JsonProperty
    @Nullable
    public abstract String title();

    @JsonProperty
    @Nullable
    public abstract String description();

    @JsonProperty
    public abstract RuleBuilder ruleBuilder();

    @JsonProperty
    @Nullable
    public abstract DateTime createdAt();

    @JsonProperty
    @Nullable
    public abstract DateTime modifiedAt();

    @JsonCreator
    public static RuleBuilderRule create(@JsonProperty("id") @Id @ObjectId @Nullable String id,
                                         @JsonProperty("title") String title,
                                         @JsonProperty("description") @Nullable String description,
                                         @JsonProperty("rulebuilder") RuleBuilder ruleBuilder,
                                         @JsonProperty("created_at") @Nullable DateTime createdAt,
                                         @JsonProperty("modified_at") @Nullable DateTime modifiedAt) {
        return builder()
                .id(id)
                .title(title)
                .description(description)
                .ruleBuilder(ruleBuilder)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_RuleBuilderRule.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder title(String title);

        public abstract Builder description(String description);

        public abstract Builder ruleBuilder(RuleBuilder ruleBuilder);

        public abstract Builder createdAt(DateTime createdAt);

        public abstract Builder modifiedAt(DateTime modifiedAt);

        public abstract RuleBuilderRule build();
    }
}
