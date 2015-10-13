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
package org.graylog2.indexer.searches.timeranges;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Seconds;

import java.util.Locale;
import java.util.Map;

public class RelativeRange implements TimeRange {

    private final int range;

    public RelativeRange(int range) throws InvalidRangeParametersException {
        if (range < 0) {
            throw new InvalidRangeParametersException();
        }

        this.range = range;
    }

    @Override
    public Type getType() {
        return Type.RELATIVE;
    }

    @Override
    public Map<String, Object> getPersistedConfig() {
        return ImmutableMap.<String, Object>of(
                "type", getType().toString().toLowerCase(Locale.ENGLISH),
                "range", getRange());
    }

    public int getRange() {
        return range;
    }

    @Override
    public DateTime getFrom() {
        if (getRange() > 0) {
            return Tools.iso8601().minus(Seconds.seconds(getRange()));
        }
        return new DateTime(0, DateTimeZone.UTC);
    }

    @Override
    public DateTime getTo() {
        return Tools.iso8601();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("range", range)
                .add("from", getFrom())
                .add("to", getTo())
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RelativeRange that = (RelativeRange) o;
        return range == that.range;
    }

    @Override
    public int hashCode() {
        return range;
    }
}
