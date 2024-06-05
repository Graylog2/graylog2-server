package org.graylog2.entitygroups.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog.events.contentpack.entities.EntityGroupEntity;
import org.graylog2.contentpacks.ContentPackable;
import org.graylog2.contentpacks.EntityDescriptorIds;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = EntityGroup.FIELD_TYPE,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        visible = true,
        defaultImpl = EntityGroup.Fallback.class)
public interface EntityGroup extends ContentPackable<EntityGroupEntity> {
    String FIELD_TYPE = "type";
    @JsonProperty(FIELD_TYPE)
    String type();

    interface Builder<SELF> {
        @JsonProperty(FIELD_TYPE)
        SELF type(String type);
    }

    class Fallback implements EntityGroup {
        @Override
        public String type() {
            throw new UnsupportedOperationException();
        }

        @Override
        public EntityGroupEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
            //TODO: implement
            return null;
        }
    }
}
