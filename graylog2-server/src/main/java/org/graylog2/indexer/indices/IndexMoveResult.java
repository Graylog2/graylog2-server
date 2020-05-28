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
package org.graylog2.indexer.indices;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class IndexMoveResult {
    public abstract int movedDocuments();
    public abstract long tookMs();
    public abstract boolean hasFailedItems();

    public static IndexMoveResult create(int movedDocuments, long tookMs, boolean hasFailedItems) {
        return new AutoValue_IndexMoveResult(movedDocuments, tookMs, hasFailedItems);
    }
}
