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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Map;

@AutoValue
@JsonAutoDetect
abstract class Widget {
    private static final String FIELD_ID = "id";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_CACHE_TIME = "cache_time";
    private static final String FIELD_CREATOR_USER_ID = "creator_user_id";
    private static final String FIELD_CONFIG = "config";

    @JsonProperty(FIELD_ID)
    abstract String id();
    @JsonProperty(FIELD_TYPE)
    abstract String type();
    @JsonProperty(FIELD_DESCRIPTION)
    abstract String description();
    @JsonProperty(FIELD_CACHE_TIME)
    abstract int cacheTime();
    @JsonProperty(FIELD_CREATOR_USER_ID)
    abstract String creatorUserId();
    @JsonProperty(FIELD_CONFIG)
    abstract Map<String, Object> config();

    @JsonCreator
    static Widget create(
            @JsonProperty(FIELD_ID) String id,
            @JsonProperty(FIELD_TYPE) String type,
            @JsonProperty(FIELD_DESCRIPTION) String description,
            @JsonProperty(FIELD_CACHE_TIME) int cacheTime,
            @JsonProperty(FIELD_CREATOR_USER_ID) String creatorUserId,
            @JsonProperty(FIELD_CONFIG) Map<String, Object> config
    ) {
        return new AutoValue_Widget(id, type, description, cacheTime, creatorUserId, config);
    }
}
