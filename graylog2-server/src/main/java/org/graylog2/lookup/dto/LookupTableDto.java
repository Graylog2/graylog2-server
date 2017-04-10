package org.graylog2.lookup.dto;

import com.google.auto.value.AutoValue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.graylog.autovalue.WithBeanGetter;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = AutoValue_LookupTableDto.Builder.class)
public abstract class LookupTableDto {

    @Id
    @ObjectId
    @Nullable
    @JsonProperty("id")
    public abstract String id();

    @JsonProperty("title")
    public abstract String title();

    @JsonProperty("description")
    public abstract String description();

    @JsonProperty("name")
    public abstract String name();

    @ObjectId
    @JsonProperty("cache")
    public abstract String cacheId();

    @ObjectId
    @JsonProperty("data_adapter")
    public abstract String dataAdapterId();

    public static Builder builder() {
        return new AutoValue_LookupTableDto.Builder();
    }


    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty("id")
        public abstract Builder id(@Nullable String id);

        @JsonProperty("title")
        public abstract Builder title(String title);

        @JsonProperty("description")
        public abstract Builder description(String description);

        @JsonProperty("name")
        public abstract Builder name(String name);

        @JsonProperty("cache")
        public abstract Builder cacheId(String id);

        @JsonProperty("data_adapter")
        public abstract Builder dataAdapterId(String id);

        public abstract LookupTableDto build();
    }
}
