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
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class FunctionDescriptor<T> {

    public abstract String name();

    public abstract boolean pure();

    public abstract Class<T> returnType();

    public abstract ImmutableList<ParameterDescriptor> params();

    public static <T> Builder<T> builder() {
        //noinspection unchecked
        return new AutoValue_FunctionDescriptor.Builder().pure(false);
    }

    @AutoValue.Builder
    public static abstract class Builder<T> {
        public abstract FunctionDescriptor<T> build();

        public abstract Builder<T> name(String name);
        public abstract Builder<T> pure(boolean pure);
        public abstract Builder<T> returnType(Class<T> type);
        public abstract Builder<T> params(ImmutableList<ParameterDescriptor> params);
    }
}
