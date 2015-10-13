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
import org.joda.time.format.ISODateTimeFormat;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class AbsoluteRange implements TimeRange {

    private final DateTime from;
    private final DateTime to;

    public AbsoluteRange(DateTime from, DateTime to) {
        this.from = checkNotNull(from);
        this.to = checkNotNull(to);
    }

    public AbsoluteRange(String from, String to) throws InvalidRangeParametersException {
        if (from == null || from.isEmpty() || to == null || to.isEmpty()) {
            throw new InvalidRangeParametersException();
        }

        try {
            if (from.contains("T")) {
                this.from = DateTime.parse(from, ISODateTimeFormat.dateTime());
            } else {
                this.from = DateTime.parse(from, Tools.timeFormatterWithOptionalMilliseconds());
            }
            if (to.contains("T")) {
                this.to = DateTime.parse(to, ISODateTimeFormat.dateTime());
            } else {
                this.to = DateTime.parse(to, Tools.timeFormatterWithOptionalMilliseconds());
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidRangeParametersException();
        }
    }

    @Override
    public Type getType() {
        return Type.ABSOLUTE;
    }

    @Override
    public Map<String, Object> getPersistedConfig() {
        return ImmutableMap.<String, Object>of(
                "type", getType().toString().toLowerCase(Locale.ENGLISH),
                "from", getFrom(),
                "to", getTo());
    }

    public DateTime getFrom() {
        return from;
    }

    public DateTime getTo() {
        return to;
    }

    public Map<String, DateTime> getLimits() {
        return ImmutableMap.of(
                "from", getFrom(),
                "to", getTo());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("from", getFrom())
                .add("to", getTo())
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbsoluteRange that = (AbsoluteRange) o;
        return from.equals(that.from) && to.equals(that.to);

    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }
}
