/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.alarmcallbacks;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.database.CollectionName;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Map;

@AutoValue
@JsonAutoDetect
@CollectionName("alarmcallbackconfigurations")
public abstract class AlarmCallbackConfigurationAVImpl implements AlarmCallbackConfiguration {
    @JsonProperty("_id")
    @ObjectId
    @Override
    public abstract String getId();

    @JsonProperty("stream_id")
    @Override
    public abstract String getStreamId();

    @JsonProperty("type")
    @Override
    public abstract String getType();

    @JsonProperty("title")
    @Override
    @Nullable
    public abstract String getTitle();

    @JsonProperty("configuration")
    @Override
    public abstract Map<String, Object> getConfiguration();

    @JsonProperty("created_at")
    @Override
    public abstract Date getCreatedAt();

    @JsonProperty("creator_user_id")
    @Override
    public abstract String getCreatorUserId();

    public abstract Builder toBuilder();

    @JsonCreator
    public static AlarmCallbackConfigurationAVImpl create(@JsonProperty("_id") String id,
                                                          @JsonProperty("stream_id") String streamId,
                                                          @JsonProperty("type") String type,
                                                          @JsonProperty("title") @Nullable String title,
                                                          @JsonProperty("configuration") Map<String, Object> configuration,
                                                          @JsonProperty("created_at") Date createdAt,
                                                          @JsonProperty("creator_user_id") String creatorUserId,
                                                          @Nullable @JsonProperty("id") String redundantId) {
        return create(id, streamId, type, title, configuration, createdAt, creatorUserId);
    }

    public static AlarmCallbackConfigurationAVImpl create(@JsonProperty("_id") String id,
                                                          @JsonProperty("stream_id") String streamId,
                                                          @JsonProperty("type") String type,
                                                          @JsonProperty("title") @Nullable String title,
                                                          @JsonProperty("configuration") Map<String, Object> configuration,
                                                          @JsonProperty("created_at") Date createdAt,
                                                          @JsonProperty("creator_user_id") String creatorUserId) {
        return new AutoValue_AlarmCallbackConfigurationAVImpl.Builder()
                .setId(id)
                .setStreamId(streamId)
                .setType(type)
                .setTitle(title)
                .setConfiguration(configuration)
                .setCreatedAt(createdAt)
                .setCreatorUserId(creatorUserId)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setId(String id);

        public abstract Builder setStreamId(String streamId);

        public abstract Builder setType(String type);

        public abstract Builder setTitle(String title);

        public abstract Builder setConfiguration(Map<String, Object> configuration);

        public abstract Builder setCreatedAt(Date createdAt);

        public abstract Builder setCreatorUserId(String creatorUserId);

        public abstract AlarmCallbackConfigurationAVImpl build();
    }
}
