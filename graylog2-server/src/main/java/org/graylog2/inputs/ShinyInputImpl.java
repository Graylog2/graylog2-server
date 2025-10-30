package org.graylog2.inputs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.database.BuildableMongoEntity;
import org.graylog2.plugin.IOState;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;

import java.util.Map;

@AutoValue
@JsonDeserialize(builder = ShinyInputImpl.Builder.class)
public abstract class ShinyInputImpl implements BuildableMongoEntity<ShinyInputImpl, ShinyInputImpl.Builder> {
    @JsonProperty("title")
    public abstract String title();

    @JsonProperty("created_at")
    public abstract DateTime createdAt();

    @JsonProperty("configuration")
    public abstract Map<String, Object> configuration();

    @JsonProperty("static_fields")
    @Nullable
    public abstract Map<String, String> staticFields();

    @JsonProperty("type")
    public abstract String type();

    @JsonProperty("creator_user_id")
    public abstract String creatorUserId();

    @JsonProperty("global")
    public abstract Boolean isGlobal();

    @JsonProperty("content_pack")
    @Nullable
    public abstract String contentPack();

    @JsonProperty("node_id")
    @Nullable
    public abstract String nodeId();

    @JsonProperty("desired_state")
    public abstract String desiredState();

    public void setDesiredState(IOState.Type desiredState) {

    }

    public IOState.Type getDesiredState() {
        final String desiredState = desiredState();
        if (StringUtils.isNotBlank(desiredState)) {
            if (EnumUtils.isValidEnum(IOState.Type.class, desiredState)) {
                return IOState.Type.valueOf(desiredState);
            }
        }
        return IOState.Type.RUNNING;
    }

    @AutoValue.Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public abstract static class Builder implements BuildableMongoEntity.Builder<ShinyInputImpl, Builder> {
        @JsonCreator
        public static Builder create() { return new AutoValue_ShinyInputImpl.Builder(); }

        @JsonProperty("title")
        public abstract Builder title(String title);

        @JsonProperty("created_at")
        public abstract Builder createdAt(DateTime createdAt);

        @JsonProperty("configuration")
        public abstract Builder configuration(Map<String, Object> configuration);

        @JsonProperty("static_fields")
        public abstract Builder staticFields(@Nullable Map<String, String> staticFields);

        @JsonProperty("type")
        public abstract Builder type(String type);

        @JsonProperty("creator_user_id")
        public abstract Builder creatorUserId(String creatorUserId);

        @JsonProperty("global")
        public abstract Builder isGlobal(Boolean isGlobal);

        @JsonProperty("content_pack")
        public abstract Builder contentPack(@Nullable String contentPack);

        @JsonProperty("node_id")
        public abstract Builder nodeId(@Nullable String nodeId);

        @JsonProperty("desired_state")
        public abstract Builder desiredState(String desiredState);
    }
}
