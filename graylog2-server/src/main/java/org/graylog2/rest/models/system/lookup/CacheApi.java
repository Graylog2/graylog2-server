package org.graylog2.rest.models.system.lookup;

import com.google.auto.value.AutoValue;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.plugin.lookup.LookupCacheConfiguration;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
@WithBeanGetter
@JsonDeserialize(builder = AutoValue_CacheApi.Builder.class)
public abstract class CacheApi {

    @Nullable
    @JsonProperty("id")
    public abstract String id();

    @JsonProperty("title")
    public abstract String title();

    @JsonProperty("description")
    public abstract String description();

    @JsonProperty("name")
    public abstract String name();

    @JsonProperty
    public abstract LookupCacheConfiguration config();

    public static Builder builder() {
        return new AutoValue_CacheApi.Builder();
    }

    public static CacheApi fromDto(CacheDto dto) {
        return builder()
                .id(dto.id())
                .title(dto.title())
                .description(dto.description())
                .name(dto.name())
                .config(dto.config())
                .build();
    }

    public CacheDto toDto() {
        return CacheDto.builder()
                .id(id())
                .title(title())
                .description(description())
                .name(name())
                .config(config())
                .build();
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

        @JsonProperty("config")
        public abstract Builder config(LookupCacheConfiguration config);

        public abstract CacheApi build();
    }
}
