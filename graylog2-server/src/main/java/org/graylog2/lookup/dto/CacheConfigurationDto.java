package org.graylog2.lookup.dto;

import com.google.auto.value.AutoValue;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.lookup.LookupCache;
import org.mongojack.Id;
import org.mongojack.MongoCollection;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@WithBeanGetter
@MongoCollection(name = "lut_caches")
public abstract class CacheConfigurationDto {

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

    public static Builder builder() {
        return new AutoValue_CacheConfigurationDto.Builder();
    }

    public static LookupCache toDomainObject(CacheConfigurationDto cacheConfigurationDto) {
        return null;
    }


    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder title(String title);

        public abstract Builder description(String description);

        public abstract Builder name(String name);

        public abstract CacheConfigurationDto build();
    }
}
