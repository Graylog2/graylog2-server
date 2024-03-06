package org.graylog2.indexer.datastream.policy.actions;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Formats byte sizes using accepted OpenSearch values.
 * <a href="https://opensearch.org/docs/latest/api-reference/units/">...</a>
 */
public enum BytesUnit {
    PEBIBYTES("pb"),
    TEBIBYTES("tb"),
    GIBIBYTES("gb"),
    MEBIBYTES("mb"),
    KIBIBYTES("kb"),
    BYTES("b");

    private final String abbreviation;

    BytesUnit(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String format(long size) {
        return f("%d" + abbreviation, size);
    }
}
