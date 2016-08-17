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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
public abstract class FunctionDescriptor<T> {

    @JsonProperty
    public abstract String name();

    @JsonProperty
    public abstract boolean pure();

    @JsonProperty
    public abstract Class<? extends T> returnType();

    @JsonProperty
    public abstract ImmutableList<ParameterDescriptor> params();

    @JsonIgnore
    public abstract ImmutableMap<String, ParameterDescriptor> paramMap();

    @JsonIgnore
    public ParameterDescriptor param(String name) {
        return paramMap().get(name);
    }

    @JsonProperty
    @Nullable
    public abstract String description();

    public static <T> Builder<T> builder() {
        //noinspection unchecked
        return new AutoValue_FunctionDescriptor.Builder().pure(false);
    }

    @AutoValue.Builder
    public static abstract class Builder<T> {
        abstract FunctionDescriptor<T> autoBuild();

        public FunctionDescriptor<T> build() {
            return paramMap(Maps.uniqueIndex(params(), ParameterDescriptor::name))
                    .autoBuild();
        }

        public abstract Builder<T> name(String name);
        public abstract Builder<T> pure(boolean pure);
        public abstract Builder<T> returnType(Class<? extends T> type);
        public Builder<T> params(ParameterDescriptor... params) {
            return params(ImmutableList.<ParameterDescriptor>builder().add(params).build());
        }
        public abstract Builder<T> params(ImmutableList<ParameterDescriptor> params);
        public abstract Builder<T> paramMap(ImmutableMap<String, ParameterDescriptor> map);
        public abstract ImmutableList<ParameterDescriptor> params();
        public abstract Builder<T> description(@Nullable String description);
    }
}
