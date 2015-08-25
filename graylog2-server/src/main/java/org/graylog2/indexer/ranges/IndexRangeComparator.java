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
package org.graylog2.indexer.ranges;

import com.google.common.collect.ComparisonChain;

import java.util.Comparator;

public class IndexRangeComparator implements Comparator<IndexRange> {
    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(IndexRange o1, IndexRange o2) {
        return ComparisonChain.start()
                .compare(o1.end(), o2.end())
                .compare(o1.begin(), o2.begin())
                .compare(o1.indexName(), o2.indexName())
                .result();
    }
}
