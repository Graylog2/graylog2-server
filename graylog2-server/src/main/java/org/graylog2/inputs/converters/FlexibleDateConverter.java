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
package org.graylog2.inputs.converters;

import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class FlexibleDateConverter extends AbstractDateConverter {
    public FlexibleDateConverter(Map<String, Object> config) {
        super(Type.FLEXDATE, config);
    }

    @Override
    @Nullable
    public Object convert(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        final Parser parser = new Parser(timeZone.toTimeZone());
        final List<DateGroup> r = parser.parse(value);

        if (r.isEmpty() || r.get(0).getDates().isEmpty()) {
            return null;
        }

        return new DateTime(r.get(0).getDates().get(0), timeZone);
    }
}
