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

import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetStats;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

public class IndexSetStatsCreator {
    private final Indices indices;

    @Inject
    public IndexSetStatsCreator(final Indices indices) {
        this.indices = indices;
    }

    public IndexSetStats getForIndexSet(final IndexSet indexSet) {
        final Map<String, IndexStats> docCounts = indices.getAllDocCounts(indexSet);
        final Set<String> closedIndices = indices.getClosedIndices(indexSet);
        final long documents = docCounts.values()
                .stream()
                .mapToLong(indexStats -> indexStats.getPrimaries().getDocs().getCount())
                .sum();
        final long size = docCounts.values()
                .stream()
                .mapToLong(indexStats -> indexStats.getPrimaries().getStore().sizeInBytes())
                .sum();

        return IndexSetStats.create(docCounts.size() + closedIndices.size(), documents, size);
    }
}
