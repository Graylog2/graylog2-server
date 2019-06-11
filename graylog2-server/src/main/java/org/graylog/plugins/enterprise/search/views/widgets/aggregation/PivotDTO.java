package org.graylog.plugins.enterprise.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@AutoValue
@JsonDeserialize(builder = PivotDTO.Builder.class)
@WithBeanGetter
public abstract class PivotDTO {
    static final String FIELD_FIELD_NAME = "field";
    static final String FIELD_TYPE = "type";
    static final String FIELD_CONFIG = "config";

    @JsonProperty(FIELD_FIELD_NAME)
    public abstract String field();

    @JsonProperty(FIELD_TYPE)
    public abstract String type();

    @JsonProperty(FIELD_CONFIG)
    public abstract PivotConfigDTO config();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_FIELD_NAME)
        public abstract Builder field(String field);

        @JsonProperty(FIELD_CONFIG)
        @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = PivotDTO.FIELD_TYPE,
                visible = true)
        public abstract Builder config(PivotConfigDTO config);

        @JsonProperty(FIELD_TYPE)
        public abstract Builder type(String type);

        public abstract PivotDTO build();

        @JsonCreator
        static Builder builder() {
            return new AutoValue_PivotDTO.Builder();
        }
    }
}
