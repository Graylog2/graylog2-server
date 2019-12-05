package org.graylog.plugins.views.search.views;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.auto.value.AutoValue;

import java.util.Map;

@AutoValue
@JsonAutoDetect
public abstract class UnknownWidgetConfigDTO implements WidgetConfigDTO {
    @JsonValue
    public abstract Map<String, Object> config();

    @JsonCreator
    public static UnknownWidgetConfigDTO create(Map<String, Object> config) {
        return new AutoValue_UnknownWidgetConfigDTO(config);
    }
}
