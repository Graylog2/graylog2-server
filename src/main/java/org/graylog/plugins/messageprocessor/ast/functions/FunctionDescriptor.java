package org.graylog.plugins.messageprocessor.ast.functions;

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
