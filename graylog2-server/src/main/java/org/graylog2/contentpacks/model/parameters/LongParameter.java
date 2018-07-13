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
import org.graylog2.contentpacks.model.entities.references.ValueType;

@AutoValue
@JsonDeserialize(builder = AutoValue_LongParameter.Builder.class)
public abstract class LongParameter implements Parameter<Long> {
    static final String TYPE_NAME = "long";

    public static Builder builder() {
        return new AutoValue_LongParameter.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder implements ParameterBuilder<Builder, Long> {
        abstract LongParameter autoBuild();

        public LongParameter build() {
            valueType(ValueType.LONG);
            return autoBuild();
        }
    }
}
