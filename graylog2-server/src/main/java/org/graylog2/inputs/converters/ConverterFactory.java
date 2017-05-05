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
import org.graylog2.lookup.LookupTableService;
import org.graylog2.plugin.inputs.Converter;

import javax.inject.Inject;
import java.util.Map;

public class ConverterFactory {
    private final LookupTableService lookupTableService;

    @Inject
    public ConverterFactory(final LookupTableService lookupTableService) {
        this.lookupTableService = lookupTableService;
    }

    public Converter create(Converter.Type type, Map<String, Object> config) throws NoSuchConverterException, ConfigurationException {
        switch (type) {
            case NUMERIC:
                return new NumericConverter(config);
            case DATE:
                return new DateConverter(config);
            case HASH:
                return new HashConverter(config);
            case SPLIT_AND_COUNT:
                return new SplitAndCountConverter(config);
            case SYSLOG_PRI_LEVEL:
                return new SyslogPriLevelConverter(config);
            case SYSLOG_PRI_FACILITY:
                return new SyslogPriFacilityConverter(config);
            case IP_ANONYMIZER:
                return new IPAnonymizerConverter(config);
            case TOKENIZER:
                return new TokenizerConverter(config);
            case CSV:
                return new CsvConverter(config);
            case LOWERCASE:
                return new LowercaseConverter(config);
            case UPPERCASE:
                return new UppercaseConverter(config);
            case FLEXDATE:
                return new FlexibleDateConverter(config);
            case LOOKUP_TABLE:
                return new LookupTableConverter(config, lookupTableService);
            default:
                throw new NoSuchConverterException();
        }
    }

    public static class NoSuchConverterException extends Exception {
    }
}
