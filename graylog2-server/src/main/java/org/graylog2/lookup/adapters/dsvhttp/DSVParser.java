package org.graylog2.lookup.adapters.dsvhttp;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class DSVParser {
    private final String ignorechar;
    private final String separator;
    private final String quoteChar;
    private final Boolean keyOnly;
    private final Boolean caseInsensitive;
    private final Integer keyColumn;
    private final Optional<Integer> valueColumn;

    public DSVParser(String ignorechar,
                     String separator,
                     String quoteChar,
                     Boolean keyOnly,
                     Boolean caseInsensitive,
                     Integer keyColumn,
                     Optional<Integer> valueColumn) {

        this.ignorechar = ignorechar;
        this.separator = separator;
        this.quoteChar = quoteChar;
        this.keyOnly = keyOnly;
        this.caseInsensitive = caseInsensitive;
        this.keyColumn = keyColumn;
        this.valueColumn = valueColumn;
    }

    public Map<String, String> parse(String body) {
        final ImmutableMap.Builder<String, String> newLookupBuilder = ImmutableMap.builder();

        final String[] lines = body.split("\n");

        for (String line : lines) {
            if (line.startsWith(this.ignorechar)) {
                continue;
            }
            final String[] values = line.split(this.separator);
            if (values.length <= Math.max(keyColumn, keyOnly ? 0 : valueColumn.orElse(0))) {
                continue;
            }
            final String key = this.caseInsensitive ? values[keyColumn].toLowerCase(Locale.ENGLISH) : values[keyColumn];
            final String value = this.keyOnly ? "" : values[valueColumn.orElseThrow(() -> new IllegalStateException("No value column and not key only parsing specified!"))].trim();
            final String finalKey = Strings.isNullOrEmpty(quoteChar) ? key.trim() : key.trim().replaceAll("^" + quoteChar + "|" + quoteChar + "$", "");
            final String finalValue = Strings.isNullOrEmpty(quoteChar) ? value.trim() : value.trim().replaceAll("^" + quoteChar + "|" + quoteChar + "$", "");
            newLookupBuilder.put(finalKey, finalValue);
        }

        return newLookupBuilder.build();
    }
}
