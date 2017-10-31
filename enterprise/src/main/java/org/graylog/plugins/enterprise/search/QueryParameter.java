package org.graylog.plugins.enterprise.search;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class QueryParameter {

    public abstract String name();

    public abstract Type type();

    public static Builder builder() {
        return new AutoValue_QueryParameter.Builder();
    }

    public static QueryParameter any(String name) {
        return QueryParameter.builder().type(Type.ANY).name(name).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder type(Type type);

        public abstract Builder name(String parameter);

        public abstract QueryParameter build();
    }

    public enum Type {
        ANY
    }
}
