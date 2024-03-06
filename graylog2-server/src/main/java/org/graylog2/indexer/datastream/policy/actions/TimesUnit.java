package org.graylog2.indexer.datastream.policy.actions;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Formats time values using accepted OpenSearch values.
 * <a href="https://opensearch.org/docs/latest/api-reference/units/">...</a>
 */
public enum TimesUnit {
    DAYS("d"),
    HOURS("h"),
    MINUTES("m"),
    SECONDS("s"),
    MILLISECONDS("ms"),
    MICROSECONDS("micros"),
    NANOSECONDS("nanos");

    private final String abbreviation;

    TimesUnit(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String format(long size) {
        return f("%d" + this.abbreviation, size);
    }
}
