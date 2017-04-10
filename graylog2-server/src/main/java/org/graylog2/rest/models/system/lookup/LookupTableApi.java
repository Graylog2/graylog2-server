package org.graylog2.rest.models.system.lookup;

import com.google.auto.value.AutoValue;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.lookup.dto.LookupTableDto;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
@WithBeanGetter
@JsonDeserialize(builder = AutoValue_LookupTableApi.Builder.class)
public abstract class LookupTableApi {

    @Nullable
    @JsonProperty("id")
    public abstract String id();

    @JsonProperty("title")
    public abstract String title();

    @JsonProperty("description")
    public abstract String description();

    @JsonProperty("name")
    public abstract String name();

    @JsonProperty("cache_id")
    public abstract String cacheId();

    @JsonProperty("data_adapter_id")
    public abstract String dataAdapterId();

    public static Builder builder() {
        return new AutoValue_LookupTableApi.Builder();
    }

    public LookupTableDto toDto() {
        return LookupTableDto.builder()
                .id(id())
                .title(title())
                .description(description())
                .name(name())
                .cacheId(cacheId())
                .dataAdapterId(dataAdapterId())
                .build();
    }

    public static LookupTableApi fromDto(LookupTableDto dto) {
        return builder()
                .id(dto.id())
                .name(dto.name())
                .title(dto.title())
                .description(dto.description())
                .cacheId(dto.cacheId())
                .dataAdapterId(dto.dataAdapterId())
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder title(String title);

        public abstract Builder description(String description);

        public abstract Builder name(String name);

        public abstract Builder dataAdapterId(String id);

        public abstract Builder cacheId(String cacheId);

        public abstract LookupTableApi build();
    }
}
