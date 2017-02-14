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

import com.google.common.primitives.Ints;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inputs.Converter;

import java.util.Map;

public class SyslogPriFacilityConverter extends Converter {
    public SyslogPriFacilityConverter(Map<String, Object> config) {
        super(Type.SYSLOG_PRI_FACILITY, config);
    }

    @Override
    public Object convert(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        final Integer priority = Ints.tryParse(value);
        if (priority == null) {
            return value;
        }

        return Tools.syslogFacilityToReadable(SyslogPriUtilities.facilityFromPriority(priority));
    }

    @Override
    public boolean buildsMultipleFields() {
        return false;
    }
}
