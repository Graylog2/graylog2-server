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
import io.searchbox.core.search.sort.Sort;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.plugins.views.search.export.ExportMessagesCommand;
import org.graylog2.plugin.Message;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static jersey.repackaged.com.google.common.collect.Lists.newArrayList;

public class SearchAfter implements RequestStrategy {

    private static final String TIEBREAKER_FIELD = Message.FIELD_GL2_MESSAGE_ID;

    private final JestWrapper jestWrapper;

    private Object[] searchAfterValues = null;

    @Inject
    public SearchAfter(JestWrapper jestWrapper) {
        this.jestWrapper = jestWrapper;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<SearchResult.Hit<Map, Void>> nextChunk(Search.Builder search, ExportMessagesCommand command) {
        SearchResult result = search(search);
        List<SearchResult.Hit<Map, Void>> hits = result.getHits(Map.class, false);
        searchAfterValues = lastHitSortFrom(hits);
        return hits;
    }

    private SearchResult search(Search.Builder search) {
        Search.Builder modified = search.addSort(timestampDescending());

        return jestWrapper.execute(modified.build(), () -> "Failed to execute Search After request");
    }

    private ArrayList<Sort> timestampDescending() {
        return newArrayList(
                new Sort("timestamp", Sort.Sorting.DESC),
                new Sort(TIEBREAKER_FIELD, Sort.Sorting.DESC)
        );
    }

    @SuppressWarnings("rawtypes")
    private Object[] lastHitSortFrom(List<SearchResult.Hit<Map, Void>> hits) {
        if (hits.isEmpty())
            return null;

        SearchResult.Hit<Map, Void> lastHit = hits.get(hits.size() - 1);

        return lastHit.sort.toArray(new Object[0]);
    }

    @Override
    public SearchSourceBuilder configure(SearchSourceBuilder ssb) {
        return searchAfterValues == null ? ssb : ssb.searchAfter(searchAfterValues);
    }
}
