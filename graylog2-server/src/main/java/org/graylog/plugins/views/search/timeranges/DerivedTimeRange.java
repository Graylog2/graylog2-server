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
package org.graylog.plugins.views.search.timeranges;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

@AutoValue
@JsonAutoDetect
public abstract class DerivedTimeRange {
    @JsonValue
    abstract TimeRange value();

    public TimeRange effectiveTimeRange(Query query, SearchType searchType) {
        if (value() instanceof DerivableTimeRange) {
            return ((DerivableTimeRange)value()).deriveTimeRange(query, searchType);
        }

        return value();
    }

    @JsonCreator
    public static DerivedTimeRange of(TimeRange timeRange) {
        return new AutoValue_DerivedTimeRange(timeRange);
    }
}
