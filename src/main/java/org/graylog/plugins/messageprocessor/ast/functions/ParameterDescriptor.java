package org.graylog.plugins.messageprocessor.ast.functions;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ParameterDescriptor {

    public abstract Class type();

    public abstract String name();

    public static Builder builder() {
        return new AutoValue_ParameterDescriptor.Builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder type(Class type);
        public abstract Builder name(String name);
        public abstract ParameterDescriptor build();
    }
}
