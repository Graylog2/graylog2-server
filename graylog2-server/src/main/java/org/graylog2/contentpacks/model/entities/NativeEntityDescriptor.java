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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.contentpacks.model.Identified;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.Typed;

/**
 * The unique description of a native entity by ID and type.
 */
@AutoValue
@JsonDeserialize(builder = AutoValue_NativeEntityDescriptor.Builder.class)
public abstract class NativeEntityDescriptor implements Identified, Typed {
    public static final String FIELD_ENTITY_ID = "entity_id";

    @JsonProperty(FIELD_ENTITY_ID)
    public abstract ModelId entityId();

    public static NativeEntityDescriptor create(ModelId entityId, ModelId id, ModelType type) {
        return builder()
                .entityId(entityId)
                .id(id)
                .type(type)
                .build();
    }

    /**
     * Shortcut for {@link #create(String, String, ModelType)}
     */
    public static NativeEntityDescriptor create(String entityId, String nativeId, ModelType type) {
        return create(ModelId.of(entityId), ModelId.of(nativeId), type);
    }

    public static Builder builder() {
        return new AutoValue_NativeEntityDescriptor.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonProperty(FIELD_ENTITY_ID)
        abstract Builder entityId(ModelId entityId);

        @JsonProperty(FIELD_META_ID)
        abstract Builder id(ModelId id);

        @JsonProperty(FIELD_META_TYPE)
        abstract Builder type(ModelType type);

        abstract NativeEntityDescriptor autoBuild();

        public NativeEntityDescriptor build() { return autoBuild(); }
    }
}
