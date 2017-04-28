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
package org.graylog2.indexer;

import com.google.gson.JsonObject;
import org.graylog2.indexer.gson.GsonUtils;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetStats;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class IndexSetStatsCreator {
    private final Indices indices;

    @Inject
    public IndexSetStatsCreator(final Indices indices) {
        this.indices = indices;
    }

    public IndexSetStats getForIndexSet(final IndexSet indexSet) {
        final Set<String> closedIndices = indices.getClosedIndices(indexSet);
        final List<JsonObject> primaries = indices.getIndexStats(indexSet).values().stream()
                .map(GsonUtils::asJsonObject)
                .map(json -> GsonUtils.asJsonObject(json.get("primaries")))
                .collect(Collectors.toList());
        final long documents = primaries.stream()
                .map(json -> GsonUtils.asJsonObject(json.get("docs")))
                .map(json -> GsonUtils.asLong(json.get("count")))
                .reduce(0L, Long::sum);
        final long size = primaries.stream()
                .map(json -> GsonUtils.asJsonObject(json.get("store")))
                .map(json -> GsonUtils.asLong(json.get("size_in_bytes")))
                .reduce(0L, Long::sum);

        return IndexSetStats.create(primaries.size() + closedIndices.size(), documents, size);
    }
}
