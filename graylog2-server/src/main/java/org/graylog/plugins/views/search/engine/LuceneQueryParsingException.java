package org.graylog.plugins.views.search.engine;

public class LuceneQueryParsingException extends Exception {
    public LuceneQueryParsingException(Throwable throwable) {
        super(throwable);
    }
}
