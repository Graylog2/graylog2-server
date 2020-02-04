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
package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.search.MessagesSearchType;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.search.SearchType;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class MessagesWidget implements ViewWidget {
    private static final String TYPE = "messages";
    private static final String FIELD_ID = "id";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_TIMERANGE = "timerange";
    private static final String FIELD_QUERY = "query";
    private static final String FIELD_STREAMS = "streams";
    private static final String FIELD_CONFIG = "config";

    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_TYPE)
    String type() {
        return TYPE;
    }

    @JsonProperty(FIELD_TIMERANGE)
    @Nullable
    TimeRange timerange() {
        return null;
    }

    @JsonProperty(FIELD_QUERY)
    @Nullable
    String query() {
        return null;
    }

    @JsonProperty(FIELD_STREAMS)
    Set<String> streams() {
        return Collections.emptySet();
    }

    @JsonProperty(FIELD_CONFIG)
    abstract MessagesWidgetConfig config();

    @Override
    public Set<SearchType> toSearchTypes(RandomUUIDProvider randomUUIDProvider) {
        return Collections.singleton(MessagesSearchType.create(randomUUIDProvider.get()));
    }

    public static MessagesWidget create(String id, List<String> fields, boolean showMessageRow) {
        return new AutoValue_MessagesWidget(id, MessagesWidgetConfig.create(fields, showMessageRow));
    }
}
