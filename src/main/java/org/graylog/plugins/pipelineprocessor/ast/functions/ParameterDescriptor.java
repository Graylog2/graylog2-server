/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.ast.functions;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ParameterDescriptor {

    public abstract Class<?> type();

    public abstract String name();

    public abstract boolean optional();

    public static Builder param() {
        return new AutoValue_ParameterDescriptor.Builder().optional(false);
    }

    public static ParameterDescriptor string(String name) {
        return param().string(name).build();
    }

    public static ParameterDescriptor object(String name) {
        return param().object(name).build();
    }

    public static ParameterDescriptor integer(String name) {
        return param().integer(name).build();
    }

    public static ParameterDescriptor floating(String name) {
        return param().floating(name).build();
    }

    public static ParameterDescriptor bool(String name) {
        return param().bool(name).build();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder type(Class<?> type);
        public abstract Builder name(String name);
        public abstract Builder optional(boolean optional);
        public abstract ParameterDescriptor build();


        public Builder string(String name) {
            return type(String.class).name(name);
        }
        public Builder object(String name) {
            return type(Object.class).name(name);
        }
        public Builder floating(String name) {
            return type(Double.class).name(name);
        }
        public Builder integer(String name) {
            return type(Long.class).name(name);
        }
        public Builder bool(String name) {
            return type(Boolean.class).name(name);
        }

        public Builder optional() {
            return optional(true);
        }

    }
}
