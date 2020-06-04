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
package org.graylog.storage.elasticsearch6.views.searchtypes;

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
