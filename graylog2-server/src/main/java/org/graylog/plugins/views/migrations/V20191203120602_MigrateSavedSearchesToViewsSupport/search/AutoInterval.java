package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class AutoInterval implements BucketInterval {
    public static final String type = "auto";

    @JsonProperty
    public String type() { return type; };

    @JsonProperty
    public abstract Double scaling();

    public static AutoInterval create(Double scaling) {
        return new AutoValue_AutoInterval(scaling);
    }
    public static AutoInterval create() {
        return create(1.0);
    }
}
