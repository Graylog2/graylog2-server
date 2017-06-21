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
package org.graylog2.indexer.results;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.ClearScroll;
import io.searchbox.core.SearchResult;
import io.searchbox.core.SearchScroll;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ScrollResult extends IndexQueryResult {
    private static final Logger LOG = LoggerFactory.getLogger(ScrollResult.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<Map<String, Object>>() {
    };

    public interface Factory {
        ScrollResult create(SearchResult initialResult, String query, List<String> fields);
    }

    private final JestClient jestClient;
    private final ObjectMapper objectMapper;
    private SearchResult initialResult;
    private final List<String> fields;
    private final String queryHash; // used in log output only
    private final long totalHits;

    private String scrollId;
    private int chunkId = 0;

    @AssistedInject
    public ScrollResult(JestClient jestClient, ObjectMapper objectMapper, @Assisted SearchResult initialResult, @Assisted String query, @Assisted List<String> fields) {
        super(query, null, initialResult.getJsonObject().path("took").asLong());
        this.jestClient = jestClient;
        this.objectMapper = objectMapper;
        this.initialResult = initialResult;
        this.fields = fields;
        totalHits = initialResult.getTotal();
        scrollId = getScrollIdFromResult(initialResult);

        final Md5Hash md5Hash = new Md5Hash(getOriginalQuery());
        queryHash = md5Hash.toHex();

        LOG.debug("[{}] Starting scroll request for query {}", queryHash, getOriginalQuery());
    }

    public ScrollChunk nextChunk() throws IOException {

        final JestResult search;
        final List<ResultMessage> hits;
        if (initialResult == null) {
            search = getNextScrollResult();
            hits = StreamSupport.stream(search.getJsonObject().path("hits").path("hits").spliterator(), false)
                    .map(hit -> ResultMessage.parseFromSource(hit.path("_id").asText(),
                            hit.path("_index").asText(),
                            objectMapper.convertValue(hit.get("_source"), MAP_TYPE_REFERENCE)))
                    .collect(Collectors.toList());
        } else {
            // make sure to return the initial hits, see https://github.com/Graylog2/graylog2-server/issues/2126
            search = initialResult;
            hits = initialResult.getHits(Map.class, false).stream()
                .map(hit -> ResultMessage.parseFromSource(hit.id, hit.index, (Map<String, Object>)hit.source))
                .collect(Collectors.toList());
            this.initialResult = null;
        }

        if (hits.size() == 0) {
            // scroll exhausted
            LOG.debug("[{}] Reached end of scroll results.", queryHash, getOriginalQuery());
            return null;
        }
        LOG.debug("[{}][{}] New scroll id {}, number of hits in chunk: {}", queryHash, chunkId, getScrollIdFromResult(search), hits.size());
        scrollId = getScrollIdFromResult(search); // save the id for the next request.

        return new ScrollChunk(hits, fields, chunkId++);
    }

    private String getScrollIdFromResult(JestResult result) {
        return result.getJsonObject().path("_scroll_id").asText();
    }

    private JestResult getNextScrollResult() throws IOException {
        final SearchScroll.Builder searchBuilder = new SearchScroll.Builder(scrollId, "1m");
        return jestClient.execute(searchBuilder.build());
    }

    public String getQueryHash() {
        return queryHash;
    }

    public long totalHits() {
        return totalHits;
    }

    public void cancel() throws IOException {
        final ClearScroll.Builder clearScrollBuilder = new ClearScroll.Builder().addScrollId(scrollId);
        final JestResult result = jestClient.execute(clearScrollBuilder.build());
        LOG.debug("[{}] clearScroll for query successful: {}", queryHash, result.isSucceeded());
    }

    public class ScrollChunk {

        private final List<ResultMessage> resultMessages;
        private List<String> fields;
        private int chunkNumber;

        ScrollChunk(List<ResultMessage> hits, List<String> fields, int chunkId) {
            this.resultMessages = hits;
            this.fields = fields;
            this.chunkNumber = chunkId;
        }

        public List<String> getFields() {
            return fields;
        }

        public int getChunkNumber() {
            return chunkNumber;
        }

        public boolean isFirstChunk() {
            return getChunkNumber() == 0;
        }

        public List<ResultMessage> getMessages() {
            return resultMessages;
        }
    }
}
