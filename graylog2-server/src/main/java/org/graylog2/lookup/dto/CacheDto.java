package org.graylog2.lookup.dto;

import com.google.auto.value.AutoValue;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.lookup.LookupCacheConfiguration;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@WithBeanGetter
public abstract class CacheDto {

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

    @JsonProperty("config")
    public abstract LookupCacheConfiguration config();

    public static Builder builder() {
        return new AutoValue_CacheDto.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder title(String title);

        public abstract Builder description(String description);

        public abstract Builder name(String name);

        public abstract Builder config(LookupCacheConfiguration config);

        public abstract CacheDto build();
    }
}
