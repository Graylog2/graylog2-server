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
package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.lookup.LookupDefaultSingleValue;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class LookupTableEntity {
    @JsonProperty("name")
    public abstract String name();

    @JsonProperty("title")
    public abstract String title();

    @JsonProperty("description")
    public abstract String description();

    @JsonProperty("cache_name")
    public abstract String cacheName();

    @JsonProperty("data_adapter_name")
    public abstract String dataAdapterName();

    @JsonProperty("default_single_value")
    public abstract String defaultSingleValue();

    @JsonProperty("default_single_value_type")
    public abstract LookupDefaultSingleValue.Type defaultSingleValueType();

    @JsonProperty("default_multi_value")
    public abstract String defaultMultiValue();

    @JsonProperty("default_multi_value_type")
    public abstract LookupDefaultSingleValue.Type defaultMultiValueType();

    @JsonCreator
    public static LookupTableEntity create(
            @JsonProperty("name") String name,
            @JsonProperty("title") String title,
            @JsonProperty("description") String description,
            @JsonProperty("cache_name") String cacheName,
            @JsonProperty("data_adapter_name") String dataAdapterName,
            @JsonProperty("default_single_value") String defaultSingleValue,
            @JsonProperty("default_single_value_type") LookupDefaultSingleValue.Type defaultSingleValueType,
            @JsonProperty("default_multi_value") String defaultMultiValue,
            @JsonProperty("default_multi_value_type") LookupDefaultSingleValue.Type defaultMultiValueType) {
        return new AutoValue_LookupTableEntity(name, title, description, cacheName, dataAdapterName, defaultSingleValue, defaultSingleValueType, defaultMultiValue, defaultMultiValueType);
    }
}
