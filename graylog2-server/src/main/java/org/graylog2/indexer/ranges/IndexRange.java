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

import org.joda.time.DateTime;

import java.util.Comparator;
import java.util.List;

public interface IndexRange {
    String FIELD_TOOK_MS = "took_ms";
    String FIELD_CALCULATED_AT = "calculated_at";
    String FIELD_END = "end";
    String FIELD_BEGIN = "begin";
    String FIELD_INDEX_NAME = "index_name";
    String FIELD_STREAM_IDS = "stream_ids";
    Comparator<IndexRange> COMPARATOR = new IndexRangeComparator();

    String indexName();

    DateTime begin();

    DateTime end();

    DateTime calculatedAt();

    int calculationDuration();

    List<String> streamIds();
}