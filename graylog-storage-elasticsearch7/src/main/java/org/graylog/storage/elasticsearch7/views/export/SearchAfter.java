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
package org.graylog.storage.elasticsearch7.views.export;

import com.google.common.collect.Streams;
import org.graylog.plugins.views.search.export.ExportMessagesCommand;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.SearchHit;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.sort.SortOrder;
import org.graylog2.plugin.Message;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class SearchAfter implements RequestStrategy {

    static final String DEFAULT_TIEBREAKER_FIELD = Message.FIELD_GL2_MESSAGE_ID;
    static final String EVENTS_TIEBREAKER_FIELD = Message.FIELD_ID;

    private final ExportClient client;

    private Object[] searchAfterValues = null;

    @Inject
    public SearchAfter(ExportClient client) {
        this.client = client;
    }

    @Override
    public List<SearchHit> nextChunk(SearchRequest search, ExportMessagesCommand command) {

        SearchResponse result = search(search);
        List<SearchHit> hits = Streams.stream(result.getHits()).collect(Collectors.toList());
        searchAfterValues = lastHitSortFrom(hits);
        return hits;
    }

    private SearchResponse search(SearchRequest search) {
        configureSort(search.source());

        return client.search(search, "Failed to execute Search After request");
    }

    private void configureSort(SearchSourceBuilder source) {
        source.sort("timestamp", SortOrder.DESC);
        source.sort(DEFAULT_TIEBREAKER_FIELD, SortOrder.DESC);
    }

    private Object[] lastHitSortFrom(List<SearchHit> hits) {
        if (hits.isEmpty())
            return null;

        SearchHit lastHit = hits.get(hits.size() - 1);

        return lastHit.getSortValues();
    }

    @Override
    public SearchSourceBuilder configure(SearchSourceBuilder ssb) {
        return searchAfterValues == null ? ssb : ssb.searchAfter(searchAfterValues);
    }
}
