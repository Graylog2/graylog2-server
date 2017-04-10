package org.graylog2.rest.models.system.lookup;

import com.google.auto.value.AutoValue;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
@WithBeanGetter
@JsonDeserialize(builder = AutoValue_DataAdapterApi.Builder.class)
public abstract class DataAdapterApi {

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
    public abstract LookupDataAdapterConfiguration config();

    public static Builder builder() {
        return new AutoValue_DataAdapterApi.Builder();
    }

    public static DataAdapterApi fromDto(DataAdapterDto dto) {
        return builder()
                .id(dto.id())
                .title(dto.title())
                .description(dto.description())
                .name(dto.name())
                .config(dto.config())
                .build();
    }

    public DataAdapterDto toDto() {
        return DataAdapterDto.builder()
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
        public abstract Builder id(String id);

        @JsonProperty("title")
        public abstract Builder title(String title);

        @JsonProperty("description")
        public abstract Builder description(String description);

        @JsonProperty("name")
        public abstract Builder name(String name);

        @JsonProperty("config")
        public abstract Builder config(LookupDataAdapterConfiguration config);

        public abstract DataAdapterApi build();
    }
}
