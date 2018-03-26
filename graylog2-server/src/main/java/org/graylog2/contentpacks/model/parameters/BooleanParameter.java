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
package org.graylog2.contentpacks.model.parameters;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.contentpacks.model.ModelType;

@AutoValue
@JsonDeserialize(builder = AutoValue_BooleanParameter.Builder.class)
public abstract class BooleanParameter implements Parameter<Boolean> {
    static final String TYPE_NAME = "boolean";

    @Override
    public Class<? extends Boolean> valueClass() {
        return Boolean.class;
    }

    public static Builder builder() {
        return new AutoValue_BooleanParameter.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder implements ParameterBuilder<Builder> {
        abstract BooleanParameter autoBuild();

        public BooleanParameter build() {
            type(ModelType.of(TYPE_NAME));
            return autoBuild();
        }
    }
}
