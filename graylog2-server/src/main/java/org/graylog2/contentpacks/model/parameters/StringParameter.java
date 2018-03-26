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
@JsonDeserialize(builder = AutoValue_StringParameter.Builder.class)
public abstract class StringParameter implements Parameter<String> {
    static final String TYPE_NAME = "string";

    @Override
    public Class<? extends String> valueClass() {
        return String.class;
    }

    public static Builder builder() {
        return new AutoValue_StringParameter.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder implements ParameterBuilder<Builder> {
        abstract StringParameter autoBuild();

        public StringParameter build() {
            type(ModelType.of(TYPE_NAME));
            return autoBuild();
        }
    }
}