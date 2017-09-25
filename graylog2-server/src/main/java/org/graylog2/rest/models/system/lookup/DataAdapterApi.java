/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.models.system.lookup;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@AutoValue
@JsonAutoDetect
@WithBeanGetter
@JsonDeserialize(builder = AutoValue_DataAdapterApi.Builder.class)
public abstract class DataAdapterApi {

    @Nullable
    @JsonProperty("id")
    public abstract String id();

    @JsonProperty("title")
    @NotEmpty
    public abstract String title();

    @JsonProperty("description")
    public abstract String description();

    @JsonProperty("name")
    @NotEmpty
    public abstract String name();

    @JsonProperty("content_pack")
    @Nullable
    public abstract String contentPack();

    @JsonProperty("config")
    @NotNull
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
                .contentPack(dto.contentPack())
                .config(dto.config())
                .build();
    }

    public DataAdapterDto toDto() {
        return DataAdapterDto.builder()
                .id(id())
                .title(title())
                .description(description())
                .name(name())
                .contentPack(contentPack())
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

        @JsonProperty("content_pack")
        public abstract Builder contentPack(@Nullable String contentPack);

        @JsonProperty("config")
        public abstract Builder config(LookupDataAdapterConfiguration config);

        public abstract DataAdapterApi build();
    }
}
