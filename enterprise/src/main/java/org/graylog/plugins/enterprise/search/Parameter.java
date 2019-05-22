package org.graylog.plugins.enterprise.search;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Maps;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

/**
 * Parameters describe variable inputs to queries.
 * <p>
 * They consist of a declaration and a binding. Parameters without a binding are called "free" or "unbound" parameters.
 * In order to execute a query all of its non-optional parameters must have a binding associated with them, i.e. be "bound".
 * <p>
 * The caller is expected to provide a {@link Parameter} object when binding previously declared parameters.
 * In that case the declaration elements do not need to be repeated, only its {@link Parameter#name() name} property.
 */
@AutoValue
@JsonDeserialize(builder = Parameter.Builder.class)
public abstract class Parameter {

    @JsonProperty
    public abstract String name();

    @Nullable
    @JsonProperty
    public abstract String title();

    @Nullable
    @JsonProperty
    public abstract String description();

    @JsonProperty("data_type")
    public abstract String dataType();

    @Nullable
    @JsonProperty("default_value")
    public abstract Object defaultValue();

    @JsonProperty
    public abstract boolean optional();

    @Nullable
    @JsonProperty
    public abstract Binding binding();

    public static Builder builder() {
        return new AutoValue_Parameter.Builder().optional(false);
    }

    public static Parameter any(String name) {
        return builder().name(name).dataType("any").build();
    }

    public abstract Builder toBuilder();

    public Parameter applyExecutionState(ObjectMapper objectMapper, JsonNode state) {
        final JsonNode bindingState = state.path(name());

        if (bindingState.isMissingNode()) {
            return this;
        }

        final Binding binding = objectMapper.convertValue(bindingState, Binding.class);

        return toBuilder().binding(binding).build();
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = Binding.TYPE_FIELD,
            visible = true,
            defaultImpl = Binding.Fallback.class)
    public interface Binding {
        String TYPE_FIELD = "type";

        @JsonProperty(TYPE_FIELD)
        String type();

        class Fallback implements Binding {
            @JsonProperty
            private String type;
            private Map<String, Object> props = Maps.newHashMap();

            @Override
            public String type() {
                return type;
            }

            @JsonAnySetter
            public void setProperties(String key, Object value) {
                props.put(key, value);
            }

            @JsonAnyGetter
            public Map<String, Object> getProperties() {
                return props;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                Binding.Fallback fallback = (Binding.Fallback) o;
                return Objects.equals(type, fallback.type) &&
                        Objects.equals(props, fallback.props);
            }

            @Override
            public int hashCode() {
                return Objects.hash(type, props);
            }
        }
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty
        public abstract Builder name(String name);

        @JsonProperty
        public abstract Builder title(String title);

        @JsonProperty
        public abstract Builder description(String description);

        @JsonProperty
        public abstract Builder dataType(String dataType);

        @JsonProperty("default_value")
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
        default Object resolve(Binding binding, Object defaultValue, Map<String, QueryResult> results) {
            return doResolve((B) binding, defaultValue, results);
        }

        Object doResolve(B binding, Object defaultValue, Map<String, QueryResult> results);
    }
}
