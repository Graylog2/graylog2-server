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
package org.graylog2.rest.models.alarmcallbacks;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Map;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class AlarmCallbackSummary {
    @JsonProperty
    public abstract String id();
    @JsonProperty("stream_id")
    public abstract String streamId();
    @JsonProperty
    public abstract String type();
    @JsonProperty("title")
    @Nullable
    public abstract String title();
    @JsonProperty
    public abstract Map<String, Object> configuration();
    @JsonProperty("created_at")
    public abstract Date createdAt();
    @JsonProperty("creator_user_id")
    public abstract String creatorUserId();

    @JsonCreator
    public static AlarmCallbackSummary create(@JsonProperty("id") String id,
                                              @JsonProperty("stream_id") String streamId,
                                              @JsonProperty("type") String type,
                                              @JsonProperty("title") @Nullable String title,
                                              @JsonProperty("configuration") Map<String, Object> configuration,
                                              @JsonProperty("created_at") Date createdAt,
                                              @JsonProperty("creator_user_id") String creatorUserId) {
        return new AutoValue_AlarmCallbackSummary(id, streamId, type, title, configuration, createdAt, creatorUserId);
    }
}
