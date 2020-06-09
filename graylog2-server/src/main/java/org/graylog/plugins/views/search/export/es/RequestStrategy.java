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
package org.graylog.plugins.views.search.export.es;

import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.plugins.views.search.export.ExportMessagesCommand;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RequestStrategy {
    @SuppressWarnings("rawtypes")
    List<SearchResult.Hit<Map, Void>> nextChunk(Search.Builder search, ExportMessagesCommand command);

    /**
     * Allows implementers to specify options on SearchSourceBuilder that cannot be specified on Search.Builder.
     *
     * @see #nextChunk(Search.Builder, ExportMessagesCommand)
     * @see org.elasticsearch.search.builder.SearchSourceBuilder#searchAfter(Object[])
     */
    default SearchSourceBuilder configure(SearchSourceBuilder ssb) {
        return ssb;
    }

    /**
     * Overriding this allows implementers to remove streams containing messages that would can't be processed.
     * Most prominently those that are lacking the required tie breaker field for search-after.
     *
     * @param streams Streams the user wants to export
     * @return Streams that can be exported using the implementing RequestStrategy
     */
    default Set<String> removeUnsupportedStreams(Set<String> streams) {
        return streams;
    }
}
