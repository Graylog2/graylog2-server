package org.graylog2.indexer;

import java.util.Collections;

public class FieldTypeException extends ElasticsearchException {
    public FieldTypeException(String msg) {
        super(msg);
    }

    public FieldTypeException(String message, String reason) {
        super(message, Collections.singletonList(reason));
    }
}
