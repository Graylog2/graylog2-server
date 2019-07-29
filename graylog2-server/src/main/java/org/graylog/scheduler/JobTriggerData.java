package org.graylog.scheduler;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = JobTriggerData.TYPE_FIELD,
        visible = true,
        defaultImpl = JobTriggerData.FallbackData.class)
public interface JobTriggerData {
    String TYPE_FIELD = "type";

    @JsonProperty(TYPE_FIELD)
    String type();

    interface Builder<SELF> {
        @JsonProperty(TYPE_FIELD)
        SELF type(String type);
    }

    class FallbackData implements JobTriggerData {
        @Override
        public String type() {
            return "";
        }
    }
}
