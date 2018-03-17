package org.graylog.plugins.enterprise.search.views;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.validation.constraints.NotBlank;

@AutoValue
@JsonDeserialize(builder = ViewClusterConfig.Builder.class)
@WithBeanGetter
public abstract class ViewClusterConfig {
    private static final String FIELD_DEFAULT_VIEW_ID = "default_view_id";

    @JsonProperty(FIELD_DEFAULT_VIEW_ID)
    @NotBlank
    public abstract String defaultViewId();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_DEFAULT_VIEW_ID)
        public abstract Builder defaultViewId(String defaultViewId);

        @JsonCreator
        public static Builder create() {
            return new AutoValue_ViewClusterConfig.Builder();
        }

        public abstract ViewClusterConfig build();
    }
}