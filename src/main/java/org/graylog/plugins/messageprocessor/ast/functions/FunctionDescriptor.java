package org.graylog.plugins.messageprocessor.ast.functions;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class FunctionDescriptor {

    public abstract String name();

    public abstract boolean pure();

    public abstract Class returnType();

    public abstract ImmutableList<ParameterDescriptor> params();

    public static Builder builder() {
        return new AutoValue_FunctionDescriptor.Builder().pure(false);
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract FunctionDescriptor build();

        public abstract Builder name(String name);
        public abstract Builder pure(boolean pure);
        public abstract Builder returnType(Class type);
        public abstract Builder params(ImmutableList<ParameterDescriptor> params);
    }
}
