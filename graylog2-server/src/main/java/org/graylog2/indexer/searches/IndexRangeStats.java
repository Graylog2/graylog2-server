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
package org.graylog2.indexer.searches;

import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@AutoValue
@WithBeanGetter
public abstract class IndexRangeStats {
    public static final IndexRangeStats EMPTY = create(new DateTime(0L, DateTimeZone.UTC), new DateTime(0L, DateTimeZone.UTC), Collections.emptyList());

    public abstract DateTime min();

    public abstract DateTime max();

    @Nullable
    public abstract List<String> streamIds();

    public static IndexRangeStats create(DateTime min, DateTime max, @Nullable List<String> streamIds) {
        return new AutoValue_IndexRangeStats(min, max, streamIds);
    }

    public static IndexRangeStats create(DateTime min, DateTime max) {
        return create(min, max, null);
    }
}
