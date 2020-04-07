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
package org.graylog.plugins.views.search.export;

import com.google.auto.value.AutoValue;

import java.util.LinkedHashMap;

@AutoValue
public abstract class SimpleMessage {
    public static SimpleMessage from(LinkedHashMap<String, Object> fieldsMap) {
        return builder().fields(fieldsMap).build();
    }

    public abstract LinkedHashMap<String, Object> fields();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder fields(LinkedHashMap<String, Object> fields);

        public static Builder create() {
            return new AutoValue_SimpleMessage.Builder();
        }

        abstract SimpleMessage autoBuild();

        public SimpleMessage build() {
            return autoBuild();
        }
    }
}
