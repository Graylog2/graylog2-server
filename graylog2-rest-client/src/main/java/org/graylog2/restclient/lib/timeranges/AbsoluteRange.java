/**
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
 */
package org.graylog2.restclient.lib.timeranges;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class AbsoluteRange extends TimeRange {

    private final String from;
    private final String to;

    public AbsoluteRange(String from, String to) throws InvalidRangeParametersException {
        if (from == null || to == null || from.isEmpty() || to.isEmpty()) {
            throw new InvalidRangeParametersException();
        }

        this.from = from;
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    @Override
    public TimeRange.Type getType() {
        return Type.ABSOLUTE;
    }

    @Override
    public Map<String, String> getQueryParams() {
        return new HashMap<String, String>() {{
            put("range_type", getType().toString().toLowerCase());
            put("from", from);
            put("to", to);
        }};
    }

    @Override
    public String toString() {
        StringBuilder sb =  new StringBuilder("Absolute time range [").append(this.getClass().getCanonicalName()).append("] - ");
        sb.append("from: ").append(this.from).append(" to: ").append(this.to);

        return sb.toString();
    }

}
