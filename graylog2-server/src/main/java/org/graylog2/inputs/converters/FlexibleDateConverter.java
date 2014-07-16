/**
 * Copyright 2014 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.inputs.converters;

import com.joestelmach.natty.DateGroup;
import org.graylog2.plugin.inputs.Converter;
import com.joestelmach.natty.Parser;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class FlexibleDateConverter extends Converter {

    public FlexibleDateConverter(Map<String, Object> config) {
        super(Type.FLEXDATE, config);
    }

    @Override
    public Object convert(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        // Parser is using local timezone with no constructor parameter passed.
        Parser parser = new Parser();
        List<DateGroup> r = parser.parse(value);

        if(r.isEmpty() || r.get(0).getDates().isEmpty()) {
            return null;
        }

        return new DateTime(r.get(0).getDates().get(0));
    }

    @Override
    public boolean buildsMultipleFields() {
        return false;
    }

}
