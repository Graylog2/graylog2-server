package org.graylog.plugins.pipelineprocessor.rulebuilder.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilder;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
public abstract class RuleBuilderDto {

    @JsonProperty("rule_id")
    @Nullable
    public abstract String ruleId();

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
    public static RuleBuilderDto create(@JsonProperty("rule_id") @Id @ObjectId @Nullable String ruleId,
                                        @JsonProperty("title") String title,
                                        @JsonProperty("description") @Nullable String description,
                                        @JsonProperty("rulebuilder") RuleBuilder ruleBuilder,
                                        @JsonProperty("created_at") @Nullable DateTime createdAt,
                                        @JsonProperty("modified_at") @Nullable DateTime modifiedAt) {
        return builder()
                .ruleId(ruleId)
                .title(title)
                .description(description)
                .ruleBuilder(ruleBuilder)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_RuleBuilderDto.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder ruleId(String ruleId);

        public abstract Builder title(String title);

        public abstract Builder description(String description);

        public abstract Builder ruleBuilder(RuleBuilder ruleBuilder);

        public abstract Builder createdAt(DateTime createdAt);

        public abstract Builder modifiedAt(DateTime modifiedAt);

        public abstract RuleBuilderDto build();
    }

}
