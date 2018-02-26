package org.graylog.plugins.enterprise.search.searchtypes.aggregation;

import java.util.Collections;

/**
 * Marker interface
 */
public interface AggregationSpec {
    String type();
    default Iterable<AggregationSpec> subAggregations() {
        return Collections.emptyList();
    }
}
