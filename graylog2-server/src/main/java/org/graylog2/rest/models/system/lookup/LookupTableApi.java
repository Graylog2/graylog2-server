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
import org.graylog2.lookup.LookupDefaultSingleValue;
import org.graylog2.lookup.dto.LookupTableDto;
import org.hibernate.validator.constraints.NotEmpty;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
@WithBeanGetter
@JsonDeserialize(builder = AutoValue_LookupTableApi.Builder.class)
public abstract class LookupTableApi {

    public static final String FIELD_DEFAULT_SINGLE_VALUE = "default_single_value";
    public static final String FIELD_DEFAULT_MULTI_VALUE = "default_multi_value";

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

    @JsonProperty("cache_id")
    @NotEmpty
    public abstract String cacheId();

    @JsonProperty("data_adapter_id")
    @NotEmpty
    public abstract String dataAdapterId();

    @JsonProperty("content_pack")
    @Nullable
    public abstract String contentPack();

    @JsonProperty(FIELD_DEFAULT_SINGLE_VALUE)
    public abstract String defaultSingleValue();

    @JsonProperty("default_single_value_type")
    public abstract LookupDefaultSingleValue.Type defaultSingleValueType();

    @JsonProperty(FIELD_DEFAULT_MULTI_VALUE)
    public abstract String defaultMultiValue();

    @JsonProperty("default_multi_value_type")
    public abstract LookupDefaultSingleValue.Type defaultMultiValueType();

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
                .contentPack(contentPack())
                .defaultSingleValue(defaultSingleValue())
                .defaultSingleValueType(defaultSingleValueType())
                .defaultMultiValue(defaultMultiValue())
                .defaultMultiValueType(defaultMultiValueType())
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
                .contentPack(dto.contentPack())
                .defaultSingleValue(dto.defaultSingleValue())
                .defaultSingleValueType(dto.defaultSingleValueType())
                .defaultMultiValue(dto.defaultMultiValue())
                .defaultMultiValueType(dto.defaultMultiValueType())
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

        @JsonProperty("cache_id")
        public abstract Builder cacheId(String cacheId);

        @JsonProperty("data_adapter_id")
        public abstract Builder dataAdapterId(String id);

        @JsonProperty("default_single_value")
        public abstract Builder defaultSingleValue(String defaultSingleValue);

        @JsonProperty("default_single_value_type")
        public abstract Builder defaultSingleValueType(LookupDefaultSingleValue.Type defaultSingleValueType);

        @JsonProperty("default_multi_value")
        public abstract Builder defaultMultiValue(String defaultMultiValue);

        @JsonProperty("default_multi_value_type")
        public abstract Builder defaultMultiValueType(LookupDefaultSingleValue.Type defaultMultiValueType);

        @JsonProperty("content_pack")
        public abstract Builder contentPack(@Nullable String contentPack);

        public abstract LookupTableApi build();
    }
}
