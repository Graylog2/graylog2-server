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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets.UnknownWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets.WidgetConfig;

import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = Widget.Builder.class)
public abstract class Widget {
    private static final String FIELD_ID = "id";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_CACHE_TIME = "cache_time";
    private static final String FIELD_CREATOR_USER_ID = "creator_user_id";
    private static final String FIELD_CONFIG = "config";

    public abstract String id();
    public abstract String type();
    public abstract String description();
    public abstract int cacheTime();
    public abstract String creatorUserId();
    public abstract WidgetConfig config();

    Set<ViewWidget> toViewWidgets(RandomUUIDProvider randomUUIDProvider) {
        return config().toViewWidgets(this, randomUUIDProvider);
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);
        @JsonProperty(FIELD_TYPE)
        public abstract Builder type(String type);
        @JsonProperty(FIELD_DESCRIPTION)
        public abstract Builder description(String description);
        @JsonProperty(FIELD_CACHE_TIME)
        public abstract Builder cacheTime(int cacheTime);
        @JsonProperty(FIELD_CREATOR_USER_ID)
        public abstract Builder creatorUserId(String creatorUserId);
        @JsonProperty(FIELD_CONFIG)
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type", defaultImpl = UnknownWidget.class)
        @JsonTypeIdResolver(WidgetConfigResolver.class)
        public abstract Builder config(WidgetConfig config);

        public abstract Widget build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_Widget.Builder();
        }
    }
}
