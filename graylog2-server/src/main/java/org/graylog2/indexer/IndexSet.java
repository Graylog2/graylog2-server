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

import com.google.common.collect.ComparisonChain;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.TooManyAliasesException;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public interface IndexSet extends Comparable<IndexSet> {
    String[] getManagedIndicesNames();

    String getWriteIndexAlias();

    String getWriteIndexWildcard();

    String getNewestTargetName() throws NoTargetIndexException;

    String getCurrentActualTargetIndex() throws TooManyAliasesException;

    Map<String,Set<String>> getAllDeflectorAliases();

    String getIndexPrefix();

    boolean isUp();

    boolean isDeflectorAlias(String index);

    boolean isManagedIndex(String index);

    void setUp();

    void cycle();

    void cleanupAliases(Set<String> indices);

    void pointTo(String shouldBeTarget, String currentTarget);

    Optional<Integer> extractIndexNumber(String index);

    IndexSetConfig getConfig();

    class IndexNameComparator implements Comparator<String> {
        private final IndexSet indexSet;

        IndexNameComparator(IndexSet indexSet) {
            this.indexSet = requireNonNull(indexSet);
        }

        @Override
        public int compare(String o1, String o2) {
            final int indexNumber1 = indexSet.extractIndexNumber(o1).orElse(-1);
            final int indexNumber2 = indexSet.extractIndexNumber(o2).orElse(-1);
            return ComparisonChain.start()
                    .compare(indexNumber1, indexNumber2)
                    .result();
        }
    }
}
