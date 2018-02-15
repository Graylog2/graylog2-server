package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Parameters describe variable inputs to queries.
 * <p>
 * They consist of a declaration and a binding. Parameters without a binding are called "free" or "unbound" parameters.
 * In order to execute a query all of its non-optional parameters must have a binding asscociated with them, i.e. be "bound".
 * <p>
 * The caller is expected to provide a {@link Parameter} object when binding previously declared parameters.
 * In that case the declaration elements do not need to be repeated, only its {@link Parameter#name() name} property.
 */
@AutoValue
@JsonDeserialize(builder = Parameter.Builder.class)
public abstract class Parameter {

    @JsonProperty
    public abstract String name();

    @JsonProperty("data_type")
    public abstract String dataType();

    @Nullable
    @JsonProperty("default")
    public abstract Object defaultValue();

    @JsonProperty
    public abstract boolean optional();

    @Nullable
    @JsonProperty
    public abstract Binding binding();

    public static Builder builder() {
        return new AutoValue_Parameter.Builder().optional(false);
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = Binding.TYPE_FIELD,
            visible = true)
    public interface Binding {

        String TYPE_FIELD = "type";

        @JsonProperty(TYPE_FIELD)
        String type();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty
        public abstract Builder name(String name);

        @JsonProperty
        public abstract Builder dataType(String dataType);

        @JsonProperty("default")
        public abstract Builder defaultValue(Object defaultValue);

        @JsonProperty
        public abstract Builder optional(boolean optional);

        @JsonProperty
        public abstract Builder binding(Binding binding);

        public abstract Parameter build();

        // to fill the default values
        @JsonCreator
        public static Builder create() {
            return Parameter.builder().optional(false);
        }
    }

    public interface BindingHandler<B extends Binding> {
        // this method only exists because the compiler cannot treat `Binding` and `B extends Binding` as the same types
        // see SearchTypeHandler
        @SuppressWarnings("unchecked")
        default Object resolve(Binding binding, Map<String, QueryResult> results) {
            return doResolve((B) binding, results);
        }

        Object doResolve(B binding, Map<String, QueryResult> results);
    }
}
