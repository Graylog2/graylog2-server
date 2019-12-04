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
package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.ElasticsearchQueryString;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.TimeRange;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class MessagesSearchType implements SearchType {
    private static final String TYPE = "messages";
    private static final String FIELD_ID = "id";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_TIMERANGE = "timerange";
    private static final String FIELD_QUERY = "query";
    private static final String FIELD_STREAMS = "streams";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_LIMIT = "limit";
    private static final String FIELD_OFFSET = "offset";

    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_TYPE)
    String type() {
        return TYPE;
    }

    @JsonProperty(FIELD_TIMERANGE)
    public Optional<TimeRange> timerange() {
        return Optional.empty();
    }

    @JsonProperty(FIELD_QUERY)
    public Optional<ElasticsearchQueryString> query() {
        return Optional.empty();
    }

    @JsonProperty(FIELD_STREAMS)
    public Set<String> streams() {
        return Collections.emptySet();
    }

    @JsonProperty(FIELD_NAME)
    public Optional<String> name() {
        return Optional.empty();
    }

    @JsonProperty(FIELD_LIMIT)
    public int limit() {
        return 150;
    }

    @JsonProperty(FIELD_OFFSET)
    public int offset() {
        return 0;
    }

    public static MessagesSearchType create(String id) {
        return new AutoValue_MessagesSearchType(id);
    }
}
