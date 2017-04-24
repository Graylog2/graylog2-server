package org.graylog2.indexer.searches;

import org.graylog2.indexer.ElasticsearchException;

public class SearchException extends ElasticsearchException {
    public SearchException(String message) {
        super(message);
    }

    public SearchException(String message, Throwable cause) {
        super(message, cause);
    }

    public SearchException(Throwable cause) {
        super(cause);
    }
}
