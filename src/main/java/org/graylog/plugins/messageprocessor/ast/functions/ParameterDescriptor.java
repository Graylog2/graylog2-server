package org.graylog.plugins.messageprocessor.ast.functions;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ParameterDescriptor {

    public abstract Class<?> type();

    public abstract String name();

    public static Builder builder() {
        return new AutoValue_ParameterDescriptor.Builder();
    }

    public static ParameterDescriptor string(String name) {
        return builder().string(name).build();
    }

    public static ParameterDescriptor object(String name) {
        return builder().name(name).type(Object.class).build();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder type(Class<?> type);
        public abstract Builder name(String name);
        public abstract ParameterDescriptor build();

        public Builder string(String name) {
            return type(String.class).name(name);
        }
    }
}
