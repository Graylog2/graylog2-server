package org.graylog.plugins.enterprise.search.errors;

public class SearchException extends RuntimeException {

    private final SearchError error;

    public SearchException(SearchError error) {
        this.error = error;
    }

    public SearchError error() {
        return error;
    }

    @Override
    public String getMessage() {
        return error.description();
    }
}
