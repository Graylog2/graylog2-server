package org.graylog.events.contentpack.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog2.contentpacks.NativeEntityConverter;
import org.graylog2.entitygroups.model.EntityGroup;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = EntityGroupEntity.TYPE_FIELD,
        visible = true)
public interface EntityGroupEntity extends NativeEntityConverter<EntityGroup> {
    String TYPE_FIELD = "type";

    @JsonProperty(TYPE_FIELD)
    String type();

    interface Builder<SELF> {
        @JsonProperty(TYPE_FIELD)
        SELF type(String type);
    }
}
