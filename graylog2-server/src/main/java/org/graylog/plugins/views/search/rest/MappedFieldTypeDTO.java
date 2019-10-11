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
package org.graylog.plugins.views.search.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.indexer.fieldtypes.FieldTypes;

@AutoValue
@JsonAutoDetect
public abstract class MappedFieldTypeDTO {
    @JsonProperty("name")
    public abstract String name();

    @JsonProperty("type")
    public abstract FieldTypes.Type type();

    @JsonCreator
    public static MappedFieldTypeDTO create(@JsonProperty("name") String name, @JsonProperty("type") FieldTypes.Type type) {
        return new AutoValue_MappedFieldTypeDTO(name, type);
    }
}
