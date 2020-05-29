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
package org.graylog2.indexer.counts;

import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

@Singleton
public class Counts {
    private final IndexSetRegistry indexSetRegistry;
    private final CountsAdapter countsAdapter;

    @Inject
    public Counts(IndexSetRegistry indexSetRegistry, CountsAdapter countsAdapter) {
        this.indexSetRegistry = indexSetRegistry;
        this.countsAdapter = countsAdapter;
    }

    public long total() {
        return totalCount(indexSetRegistry.getManagedIndices());
    }

    public long total(final IndexSet indexSet) {
        return totalCount(indexSet.getManagedIndices());
    }

    private long totalCount(final String[] indexNames) {
        // Return 0 if there are no indices in the given index set. If we run the query with an empty index list,
        // Elasticsearch will count all documents in all indices and thus return a wrong count.
        if (indexNames.length == 0) {
            return 0L;
        }

        final List<String> indices = Arrays.asList(indexNames);
        return countsAdapter.totalCount(indices);
    }
}
