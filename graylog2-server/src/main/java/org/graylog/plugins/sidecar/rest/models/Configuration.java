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
package org.graylog.plugins.sidecar.rest.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
public abstract class Configuration {
    public static final String FIELD_ID = "id";
    public static final String FIELD_COLLECTOR_ID = "collector_id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_COLOR = "color";
    public static final String FIELD_TEMPLATE = "template";

    @Id
    @ObjectId
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_COLLECTOR_ID)
    public abstract String collectorId();

    @JsonProperty(FIELD_NAME)
    public abstract String name();

    @JsonProperty(FIELD_COLOR)
    public abstract String color();

    @JsonProperty(FIELD_TEMPLATE)
    public abstract String template();

    @JsonCreator
    public static Configuration create(@JsonProperty(FIELD_ID) String id,
                                       @JsonProperty(FIELD_COLLECTOR_ID) String collectorId,
                                       @JsonProperty(FIELD_NAME) String name,
                                       @JsonProperty(FIELD_COLOR) String color,
                                       @JsonProperty(FIELD_TEMPLATE) String template) {
        return new AutoValue_Configuration(id, collectorId, name, color, template);
    }

    public static Configuration create(String collectorId,
                                       String name,
                                       String color,
                                       String template) {
        return create(new org.bson.types.ObjectId().toHexString(),
                collectorId,
                name,
                color,
                template);
    }
}
