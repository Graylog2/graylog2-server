/**
 * This file is part of Graylog.
 * <p>
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.storage.elasticsearch7.views.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import org.graylog.plugins.views.search.export.ExportMessagesCommand;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.ClearScrollRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.ClearScrollResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchScrollRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.SearchHit;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class Scroll implements RequestStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(Scroll.class);

    private static final String SCROLL_TIME = "1m";

    private final ObjectMapper objectMapper;
    private final ElasticsearchClient client;
    private String currentScrollId;

    @Inject
    public Scroll(ObjectMapper objectMapper, ElasticsearchClient client) {
        this.objectMapper = objectMapper;
        this.client = client;
    }

    @Override
    public List<SearchHit> nextChunk(SearchRequest search, ExportMessagesCommand command) {
        List<SearchHit> hits = retrieveHits(search);

        if (hits.isEmpty()) {
            cancelScroll();
        }

        return hits;
    }

    private List<SearchHit> retrieveHits(SearchRequest search) {
        if (isFirstRequest()) {
            SearchResponse result = search(search);
            currentScrollId = scrollIdFrom(result);
            return Streams.stream(result.getHits())
                    .collect(Collectors.toList());
        } else {
            SearchResponse result = continueScroll(currentScrollId);
            currentScrollId = scrollIdFrom(result);
            return hitsFrom(result);
        }
    }

    private List<SearchHit> hitsFrom(SearchResponse result) {
        return Streams.stream(result.getHits())
                .collect(Collectors.toList());
    }

    private boolean isFirstRequest() {
        return currentScrollId == null;
    }

    private void cancelScroll() {
        final ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(currentScrollId);

        final ClearScrollResponse response = client.execute((c, requestOptions) -> c.clearScroll(clearScrollRequest, requestOptions),
                "Failed to cancel scroll " + currentScrollId);
        if (!response.isSucceeded()) {
            LOG.error("Failed to cancel scroll with id " + currentScrollId);
        }
    }

    private SearchResponse search(SearchRequest search) {
        return client.search(search, "Failed to execute initial Scroll request");
    }

    private SearchResponse continueScroll(String scrollId) {
        final SearchScrollRequest request = new SearchScrollRequest(scrollId)
                .scroll(SCROLL_TIME);
        return client.execute((c, requestOptions) -> c.scroll(request, requestOptions),
                "Failed to execute Scroll request with scroll id " + currentScrollId);
    }

    private String scrollIdFrom(SearchResponse result) {
        return result.getScrollId();
    }
}
