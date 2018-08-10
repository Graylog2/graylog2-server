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
import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;

@AutoValue
@JsonDeserialize(builder = ModelType.Builder.class)
public abstract class ModelType {
    private static final String FIELD_NAME = "name";
    private static final String FIELD_VERSION = "version";

    @JsonProperty(FIELD_NAME)
    public abstract String name();

    @JsonProperty(FIELD_VERSION)
    public abstract String version();

    public static ModelType of(String name, String version) {
        validate(name, version);
        return Builder.create().name(name).version(version).build();
    }

    private static void validate(String name, String version) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "Type name must not be blank");
        Preconditions.checkArgument(StringUtils.isNotBlank(version), "Type version must not be blank");
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_ModelType.Builder();
        }

        @JsonProperty(FIELD_NAME)
        public abstract Builder name(String name);

        @JsonProperty(FIELD_VERSION)
        public abstract Builder version(String version);

        public abstract ModelType autoBuild();

        public ModelType build() {
            final ModelType modelType = autoBuild();
            validate(modelType.name(), modelType.version());
            return modelType;
        }
    }
}
