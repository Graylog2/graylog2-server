package org.graylog.events.contentpack.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog2.contentpacks.model.ContentPackV1;
import org.graylog2.contentpacks.model.LegacyContentPack;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = EventProcessorConfigEntity.TYPE_FIELD,
        visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = AggregationEventProcessorConfigEntity.class, name = AggregationEventProcessorConfigEntity.TYPE_NAME)
})
public interface EventProcessorConfigEntity {
    String TYPE_FIELD = "type";

    @JsonProperty(TYPE_FIELD)
    String type();

    interface Builder<SELF> {
        @JsonProperty(TYPE_FIELD)
        SELF type(String type);
    }
}
