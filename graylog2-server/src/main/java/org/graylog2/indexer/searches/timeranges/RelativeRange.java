/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.graylog2.indexer.searches.timeranges;

import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
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
        return new HashMap<String, Object>() {{
            put("type", getType().toString().toLowerCase());
            put("range", getRange());
        }};
    }

    public int getRange() {
        return range;
    }

    @Override
    public DateTime getFrom() {
        int from = 0;
        if (getRange() > 0) {
            from = Tools.getUTCTimestamp()-getRange();
        }

        return new DateTime(from*1000L);
    }
}
