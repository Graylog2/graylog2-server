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
package org.graylog2.bundles;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.lookup.LookupDefaultSingleValue;

@JsonAutoDetect
public class LookupTableBundle {
    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("name")
    private String name;

    @JsonProperty("cache_name")
    private String cacheName;

    @JsonProperty("data_adapter_name")
    private String dataAdapterName;

    @JsonProperty("default_single_value")
    private String defaultSingleValue;

    @JsonProperty("default_single_value_type")
    private LookupDefaultSingleValue.Type defaultSingleValueType;

    @JsonProperty("default_multi_value")
    private String defaultMultiValue;

    @JsonProperty("default_multi_value_type")
    public LookupDefaultSingleValue.Type defaultMultiValueType;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public String getDataAdapterName() {
        return dataAdapterName;
    }

    public void setDataAdapterName(String dataAdapterName) {
        this.dataAdapterName = dataAdapterName;
    }

    public String getDefaultSingleValue() {
        return defaultSingleValue;
    }

    public void setDefaultSingleValue(String defaultSingleValue) {
        this.defaultSingleValue = defaultSingleValue;
    }

    public LookupDefaultSingleValue.Type getDefaultSingleValueType() {
        return defaultSingleValueType;
    }

    public void setDefaultSingleValueType(LookupDefaultSingleValue.Type defaultSingleValueType) {
        this.defaultSingleValueType = defaultSingleValueType;
    }

    public String getDefaultMultiValue() {
        return defaultMultiValue;
    }

    public void setDefaultMultiValue(String defaultMultiValue) {
        this.defaultMultiValue = defaultMultiValue;
    }

    public LookupDefaultSingleValue.Type getDefaultMultiValueType() {
        return defaultMultiValueType;
    }

    public void setDefaultMultiValueType(LookupDefaultSingleValue.Type defaultMultiValueType) {
        this.defaultMultiValueType = defaultMultiValueType;
    }
}
