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
package org.graylog2.inputs.converters;

import org.graylog2.ConfigurationException;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inputs.Converter;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DateConverter extends Converter {

    private static final Logger LOG = LoggerFactory.getLogger(DateConverter.class);

    private final String dateFormat;

    public DateConverter(Map<String, Object> config) throws ConfigurationException {
        super(Type.DATE, config);

        if (config.get("date_format") == null || ((String) config.get("date_format")).isEmpty()) {
            throw new ConfigurationException("Missing config [date_format].");
        }

        dateFormat = ((String) config.get("date_format")).trim();
    }

    @Override
    public Object convert(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        final DateTime localNow = Tools.iso8601();

        LOG.debug("Trying to parse date <{}> with pattern <{}>.", value, dateFormat);

        return DateTime.parse(value, DateTimeFormat.forPattern(dateFormat).withDefaultYear(localNow.getYear()));
    }

    @Override
    public boolean buildsMultipleFields() {
        return false;
    }
}
