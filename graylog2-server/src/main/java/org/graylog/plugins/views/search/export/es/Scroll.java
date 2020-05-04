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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.emory.mathcs.backport.java.util.Collections;
import io.searchbox.client.JestResult;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.SearchScroll;
import io.searchbox.core.search.sort.Sort;
import io.searchbox.params.Parameters;
import org.graylog.plugins.views.search.export.ExportMessagesCommand;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.collect.Lists.newArrayList;

public class Scroll implements RequestStrategy {

    private static final String SCROLL_TIME = "1m";

    private final ObjectMapper objectMapper;
    private final JestWrapper jestWrapper;
    private String currentScrollId;

    @Inject
    public Scroll(ObjectMapper objectMapper, JestWrapper jestWrapper) {
        this.objectMapper = objectMapper;
        this.jestWrapper = jestWrapper;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<SearchResult.Hit<Map, Void>> nextChunk(Search.Builder search, ExportMessagesCommand command) {
        if (isFirstRequest()) {
            SearchResult result = search(search);
            currentScrollId = scrollIdFrom(result);
            return result.getHits(Map.class, false);
        } else {
            JestResult result = continueScroll(currentScrollId);
            currentScrollId = scrollIdFrom(result);
            return hitsFrom(result);
        }
    }

    @SuppressWarnings("rawtypes")
    private List<SearchResult.Hit<Map, Void>> hitsFrom(JestResult result) {
        return StreamSupport.stream(result.getJsonObject().path("hits").path("hits").spliterator(), false)
                .map(this::hitFromSource)
                .collect(Collectors.toList());
    }

    private boolean isFirstRequest() {
        return currentScrollId == null;
    }

    private SearchResult search(Search.Builder search) {
        Search.Builder modified = search
                .setParameter(Parameters.SCROLL, "1m")
                .addSort(unsorted());

        return jestWrapper.execute(modified.build(), () -> "Failed to execute Scroll request");
    }

    private Sort unsorted() {
        return new Sort("_doc", Sort.Sorting.ASC);
    }

    private JestResult continueScroll(String scrollId) {
        SearchScroll scroll = new SearchScroll.Builder(scrollId, SCROLL_TIME).build();

        return jestWrapper.execute(scroll, () -> "Failed to execute Scroll request");
    }

    private String scrollIdFrom(JestResult result) {
        return result.getJsonObject().path("_scroll_id").asText();
    }

    @SuppressWarnings("rawtypes")
    private SearchResult.Hit<Map, Void> hitFromSource(JsonNode hit) {
        String index = hit.path("_index").asText();

        // unfortunately `index` can only be set with the full constructor.
        // so we just use `null`/empty collections for the rest.
        // it's a bit messy, but this entire class should disappear once we manage to use
        // search after with indices that are missing GL2_MESSAGE_ID
        //noinspection unchecked
        return new SearchResult(objectMapper).new Hit<Map, Void>(
                Map.class, hit.get("_source"),
                Void.class, null,
                Collections.emptyMap(),
                newArrayList(),
                index, null, null, null);
    }
}
