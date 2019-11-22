package org.graylog.plugins.views.search.elasticsearch.searchtypes;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.core.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class MockSearchResult extends SearchResult {
    private final List<Map<String, Object>> hits;
    private final Long total;

    MockSearchResult(List<Map<String, Object>> hits, Long total) {
        super((ObjectMapper)null);
        this.hits = hits;
        this.total = total;
    }

    @Override
    public Long getTotal() {
        return this.total;
    }

    @Override
    public <T> List<Hit<T, Void>> getHits(Class<T> sourceType, boolean addEsMetadataFields) {
        final List<Hit<T, Void>> results = new ArrayList<>(this.hits.size());
        this.hits.forEach(hit -> results.add(new Hit<T, Void>((T)this.hits)));
        return results;
    }
}
