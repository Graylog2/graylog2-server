package org.graylog2.rest.resources.system.indexer;

import org.graylog2.indexer.indexset.IndexSetConfig;

import java.util.stream.Stream;

public interface OpenIndexSetFilter {

    Stream<IndexSetConfig> apply(Stream<IndexSetConfig> indexSets);
}
