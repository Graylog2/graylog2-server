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
package org.graylog.plugins.views.migrations.V20200204122000_MigrateUntypedViewsToDashboards;

import org.bson.Document;

import java.util.Set;

class SearchType {
    private static final String FIELD_FILTER = "filter";
    private static final String FIELD_QUERY = "query";
    private static final String FIELD_TIMERANGE = "timerange";
    private static final String FIELD_STREAMS = "streams";
    private final Document searchTypeDocument;

    SearchType(Document searchTypeDocument) {
        this.searchTypeDocument = searchTypeDocument;
    }

    public void syncWithWidget(Widget widget) {
        this.searchTypeDocument.remove(FIELD_FILTER);
        widget.query().ifPresent(this::setQuery);
        widget.timerange().ifPresent(this::setTimerange);
        setStreams(widget.streams());
    }

    private void setQuery(Document query) {
        this.searchTypeDocument.put(FIELD_QUERY, query);
    }

    private void setTimerange(Document timerange) {
        this.searchTypeDocument.put(FIELD_TIMERANGE, timerange);
    }

    private void setStreams(Set<String> streams) {
        this.searchTypeDocument.put(FIELD_STREAMS, streams);
    }
}
