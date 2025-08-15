package org.graylog2.database.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import org.mongojack.ObjectId;

import java.util.Optional;

@AutoValue
@JsonDeserialize(builder = EntitySource.Builder.class)
public abstract class EntitySource {
    public static final String USER_DEFINED = "USER_DEFINED";
    public static final String CONTENT_PACK = "CONTENT_PACK";

    public static final String FIELD_SOURCE = "source";
    public static final String FIELD_ENTITY_ID = "entity_id";
    public static final String FIELD_PARENT_ID = "parent_id";

    @Nullable
    @ObjectId
    @JsonProperty(value = FIELD_ENTITY_ID, access = JsonProperty.Access.WRITE_ONLY)
    public abstract String entityId();

    @JsonProperty(FIELD_SOURCE)
    public abstract String source();

    @JsonProperty(FIELD_PARENT_ID)
    public abstract Optional<String> parentId();

    public boolean isCloned() {
        return parentId().isPresent();
    }

    public static EntitySource defaultSource() {
        return Builder.create().build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_EntitySource.Builder()
                    .source(USER_DEFINED);
        }

        @JsonProperty(FIELD_ENTITY_ID)
        public abstract Builder entityId(String entityId);

        @JsonProperty(FIELD_SOURCE)
        public abstract Builder source(String source);

        @JsonProperty(FIELD_PARENT_ID)
        public abstract Builder parentId(String parentId);

        public abstract EntitySource build();
    }
}
