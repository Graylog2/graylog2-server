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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.RandomUUIDProvider;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.SearchType;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@AutoValue
public abstract class NonImplementedWidget implements ViewWidget {
    public static final String FIELD_ID = "id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_CREATOR_USER_ID = "creator_user_id";
    public static final String FIELD_CONFIG = "config";

    @JsonProperty(FIELD_ID)
    public abstract String id();
    @JsonProperty(FIELD_TYPE)
    public abstract String type();
    @JsonProperty(FIELD_CONFIG)
    public abstract Map<String, Object> config();

    @Override
    public Set<SearchType> toSearchTypes(RandomUUIDProvider randomUUIDProvider) {
        return Collections.emptySet();
    }

    @JsonCreator
    public static NonImplementedWidget create(
            @JsonProperty(FIELD_ID) String id,
            @JsonProperty(FIELD_TYPE) String type,
            @JsonProperty(FIELD_CONFIG) Map<String, Object> config
    ) {
        return new AutoValue_NonImplementedWidget(id, type, config);
    }
}
