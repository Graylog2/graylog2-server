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
public class AbsoluteRange implements TimeRange, FromToRange {

    private final DateTime from;
    private final DateTime to;

    public AbsoluteRange(String from, String to) throws InvalidRangeParametersException {
        if (from == null || from.isEmpty() || to == null || to.isEmpty()) {
            throw new InvalidRangeParametersException();
        }

        try {
            this.from = DateTime.parse(from, Tools.timeFormatterWithOptionalMilliseconds());
            this.to = DateTime.parse(to, Tools.timeFormatterWithOptionalMilliseconds());
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
        return new HashMap<String, Object>() {{
            put("type", getType().toString().toLowerCase());
            put("from", getFrom());
            put("to", getTo());
        }};
    }

    public DateTime getFrom() {
        return from;
    }

    public DateTime getTo() {
        return to;
    }

}
