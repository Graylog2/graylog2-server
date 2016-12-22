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

import org.graylog2.ConfigurationException;
import org.graylog2.plugin.inputs.Converter;

import java.util.Map;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SplitAndCountConverter extends Converter {
    private final String splitByEscaped;

    public SplitAndCountConverter(Map<String, Object> config) throws ConfigurationException {
        super(Type.SPLIT_AND_COUNT, config);

        final String splitBy = (String) config.get("split_by");
        if (isNullOrEmpty(splitBy)) {
            throw new ConfigurationException("Missing config [split_by].");
        }

        splitByEscaped = Pattern.quote(splitBy);
    }

    @Override
    public Object convert(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }

        return value.split(splitByEscaped).length;
    }

    @Override
    public boolean buildsMultipleFields() {
        return false;
    }

}
