package org.graylog.failure;

import org.graylog2.indexer.IndexFailure;

public class IndexingFailure implements Failure{

    private final IndexFailure internalFailure;

    public IndexingFailure(IndexFailure internalFailure) {
        this.internalFailure = internalFailure;
    }

    public IndexFailure getInternalFailure() {
        return internalFailure;
    }

    @Override
    public String type() {
        return "indexing";
    }

    @Override
    public String toString() {
        return internalFailure.asMap().toString();
    }
}
