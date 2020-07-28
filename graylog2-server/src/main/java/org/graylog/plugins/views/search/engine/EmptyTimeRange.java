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
package org.graylog.plugins.views.search.engine;

import org.graylog.plugins.views.search.Query;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;

/**
 * Dummy class to allow constructing an empty {@link Query query instance}.
 */
public class EmptyTimeRange extends TimeRange {

    private static final EmptyTimeRange INSTANCE = new EmptyTimeRange();

    @Override
    public String type() {
        return "empty";
    }

    @Override
    public DateTime getFrom() {
        return null;
    }

    @Override
    public DateTime getTo() {
        return null;
    }

    public static TimeRange emptyTimeRange() {
        return INSTANCE;
    }
}
