package org.graylog.scheduler;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = JobDefinitionConfig.TYPE_FIELD,
        visible = true,
        defaultImpl = JobDefinitionConfig.FallbackConfig.class)
public interface JobDefinitionConfig {
    String TYPE_FIELD = "type";

    @JsonProperty(TYPE_FIELD)
    String type();

    interface Builder<SELF> {
        @JsonProperty(TYPE_FIELD)
        SELF type(String type);
    }

    class FallbackConfig implements JobDefinitionConfig {
        @Override
        public String type() {
            throw new UnsupportedOperationException();
        }
    }
}
