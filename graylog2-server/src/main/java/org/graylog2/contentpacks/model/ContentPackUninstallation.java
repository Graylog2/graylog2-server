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
package org.graylog2.contentpacks.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;

@AutoValue
@JsonDeserialize(builder = ContentPackUninstallation.Builder.class)
public abstract class ContentPackUninstallation {
    private static final String FIELD_ENTITIES = "entities";
    private static final String FIELD_FAILED_ENTITIES = "failed_entities";

    @JsonProperty(FIELD_ENTITIES)
    public abstract ImmutableSet<NativeEntityDescriptor> entities();

    @JsonProperty(FIELD_FAILED_ENTITIES)
    public abstract ImmutableSet<NativeEntityDescriptor> failedEntities();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_ContentPackUninstallation.Builder();
        }

        @JsonProperty(FIELD_ENTITIES)
        public abstract Builder entities(ImmutableSet<NativeEntityDescriptor> entities);

        @JsonProperty(FIELD_FAILED_ENTITIES)
        public abstract Builder failedEntities(ImmutableSet<NativeEntityDescriptor> failedEntities);

        public abstract ContentPackUninstallation build();
    }
}