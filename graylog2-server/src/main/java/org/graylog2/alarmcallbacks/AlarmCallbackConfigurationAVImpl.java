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
import org.joda.time.DateTime;
import org.mongojack.ObjectId;

import java.util.Map;

@AutoValue
@JsonAutoDetect
@CollectionName("alarmcallbackconfiguration")
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

    @JsonProperty("configuration")
    @Override
    public abstract Map<String, Object> getConfiguration();

    @JsonProperty("created_at")
    @Override
    public abstract DateTime getCreatedAt();

    @JsonProperty("creator_user_id")
    @Override
    public abstract String getCreatorUserId();

    @JsonCreator
    public static AlarmCallbackConfigurationAVImpl create(@JsonProperty("_id") String id,
                                                          @JsonProperty("stream_id") String streamId,
                                                          @JsonProperty("type") String type,
                                                          @JsonProperty("configuration") Map<String, Object> configuration,
                                                          @JsonProperty("created_at") DateTime createdAt,
                                                          @JsonProperty("creator_user_id") String creatorUserId) {
        return new AutoValue_AlarmCallbackConfigurationAVImpl(id, streamId, type, configuration, createdAt, creatorUserId);
    }
}
