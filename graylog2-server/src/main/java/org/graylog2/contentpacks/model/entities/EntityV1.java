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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.contentpacks.model.ModelVersion;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = AutoValue_EntityV1.Builder.class)
public abstract class EntityV1 implements Entity {
    public static final String VERSION = "1";
    public static final String FIELD_DATA = "data";

    // TODO: Use more type-safe way to represent entity configuration?
    @JsonProperty(FIELD_DATA)
    public abstract JsonNode data();

    public static Builder builder() {
        return new AutoValue_EntityV1.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder implements EntityBuilder<Builder> {
        @JsonProperty(FIELD_DATA)
        public abstract Builder data(JsonNode data);

        public abstract EntityV1 autoBbuild();

        public EntityV1 build() {
            version(ModelVersion.of(VERSION));
            return autoBbuild();
        }
    }
}
