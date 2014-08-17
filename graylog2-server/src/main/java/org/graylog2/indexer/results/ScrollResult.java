/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.results;

import org.apache.shiro.crypto.hash.Md5Hash;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ScrollResult extends IndexQueryResult {
    private static final Logger LOG = LoggerFactory.getLogger(ScrollResult.class);

    private final Client client;
    private final List<String> fields;
    private final String queryHash; // used in log output only
    private final long totalHits;

    private String scrollId;
    private int chunkId = 0;

    public ScrollResult(Client client,
                        String originalQuery,
                        BytesReference builtQuery,
                        SearchResponse response, List<String> fields) {
        super(originalQuery, builtQuery, response.getTook());
        this.client = client;
        this.fields = fields;
        totalHits = response.getHits().totalHits();
        scrollId = response.getScrollId();

        final Md5Hash md5Hash = new Md5Hash(getOriginalQuery());
        queryHash = md5Hash.toHex();

        LOG.debug("[{}] Starting scroll request for query {}", queryHash, getOriginalQuery());
    }

    public ScrollChunk nextChunk() {
        final SearchResponse search = client.prepareSearchScroll(scrollId)
                .setScroll(TimeValue.timeValueMinutes(1))
                .execute()
                .actionGet();

        final SearchHits hits = search.getHits();
        if (hits.getHits().length == 0) {
            // scroll exhausted
            LOG.debug("[{}] Reached end of scroll results.", queryHash, getOriginalQuery());
            return null;
        }
        LOG.debug("[{}] New scroll id {}", queryHash, search.getScrollId());
        scrollId = search.getScrollId(); // save the id for the next request.

        return new ScrollChunk(hits, fields, chunkId++);
    }

    public String getQueryHash() {
        return queryHash;
    }

    public long totalHits() {
        return totalHits;
    }

    public void cancel() {
        final ClearScrollResponse clearScrollResponse = client.prepareClearScroll().addScrollId(scrollId).execute().actionGet();
        LOG.debug("[{}] clearScroll for query successful: {}", queryHash, clearScrollResponse.isSucceeded());
    }

    public class ScrollChunk {

        private final List<ResultMessage> resultMessages;
        private List<String> fields;
        private int chunkNumber;

        public ScrollChunk(SearchHits hits, List<String> fields, int chunkId) {
            this.fields = fields;
            this.chunkNumber = chunkId;
            resultMessages = buildResults(hits);
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
