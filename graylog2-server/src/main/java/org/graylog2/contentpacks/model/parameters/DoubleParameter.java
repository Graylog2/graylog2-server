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

@AutoValue
@JsonDeserialize(builder = AutoValue_DoubleParameter.Builder.class)
public abstract class DoubleParameter implements Parameter<Double> {
    static final String TYPE_NAME = "double";

    @Override
    public Class<? extends Double> valueClass() {
        return Double.class;
    }

    public static Builder builder() {
        return new AutoValue_DoubleParameter.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder implements ParameterBuilder<Builder> {
        abstract DoubleParameter autoBuild();

        public DoubleParameter build() {
            type(TYPE_NAME);
            return autoBuild();
        }
    }
}
