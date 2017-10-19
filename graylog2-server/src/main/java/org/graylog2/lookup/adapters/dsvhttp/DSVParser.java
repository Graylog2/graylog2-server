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
package org.graylog2.lookup.adapters.dsvhttp;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The main difference to using a CSVReader is that this explicitly handles comment lines and does not support
 * a column name line.
 */
public class DSVParser {
    private final String ignorechar;
    private final String lineSeparator;
    private final String quoteChar;
    private final boolean keyOnly;
    private final boolean caseInsensitive;
    private final int keyColumn;
    private final int valueColumn;

    private final String splitPattern;

    public DSVParser(String ignorechar,
                     String lineSeparator,
                     String separator,
                     String quoteChar,
                     boolean keyOnly,
                     boolean caseInsensitive,
                     int keyColumn,
                     @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Integer> valueColumn) {

        this.ignorechar = ignorechar;
        this.lineSeparator = lineSeparator;
        this.quoteChar = quoteChar;
        this.keyOnly = keyOnly;
        this.caseInsensitive = caseInsensitive;
        this.keyColumn = keyColumn;
        this.valueColumn = valueColumn.orElse(0);

        if (!keyOnly) {
            //noinspection ResultOfMethodCallIgnored
            valueColumn.orElseThrow(() -> new IllegalStateException("No value column and not key only parsing specified!"));
        }

        if (Strings.isNullOrEmpty(quoteChar)) {
            this.splitPattern = separator;
        } else {
            this.splitPattern = separator + "(?=(?:[^\\" + quoteChar + "]*\\" + quoteChar + "[^\\" + quoteChar + "]*\\" + quoteChar + ")*[^\\" + quoteChar + "]*$)";
        }
    }

    public Map<String, String> parse(String body) {
        final ImmutableMap.Builder<String, String> newLookupBuilder = ImmutableMap.builder();

        final String[] lines = body.split(lineSeparator);

        for (String line : lines) {
            if (line.startsWith(this.ignorechar)) {
                continue;
            }
            final String[] values = line.split(this.splitPattern);
            if (values.length <= Math.max(keyColumn, keyOnly ? 0 : valueColumn)) {
                continue;
            }
            final String key = this.caseInsensitive ? values[keyColumn].toLowerCase(Locale.ENGLISH) : values[keyColumn];
            final String value = this.keyOnly ? "" : values[valueColumn].trim();
            final String finalKey = Strings.isNullOrEmpty(quoteChar) ? key.trim() : key.trim().replaceAll("^" + quoteChar + "|" + quoteChar + "$", "");
            final String finalValue = Strings.isNullOrEmpty(quoteChar) ? value.trim() : value.trim().replaceAll("^" + quoteChar + "|" + quoteChar + "$", "");
            newLookupBuilder.put(finalKey, finalValue);
        }

        return newLookupBuilder.build();
    }
}
