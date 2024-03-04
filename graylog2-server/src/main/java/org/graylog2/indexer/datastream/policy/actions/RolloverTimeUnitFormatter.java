package org.graylog2.indexer.datastream.policy.actions;

public class RolloverTimeUnitFormatter {

    private RolloverTimeUnitFormatter() {
    }

    /**
     * Formats retention value in days using the proper OpenSearch time value format (e.g. 14d).
     * <a href="https://opensearch.org/docs/latest/im-plugin/ism/policies/#transitions">...</a>
     */
    public static String formatDays(long days) {
        return String.format("%dd", days);
    }
}
