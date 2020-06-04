package org.graylog2.indexer.fieldtypes;

import com.codahale.metrics.Timer;

import java.util.Map;
import java.util.Optional;

public interface IndexFieldTypePollerAdapter {
    Optional<Map<String, String>> pollIndex(String indexName, Timer pollTimer);
}
