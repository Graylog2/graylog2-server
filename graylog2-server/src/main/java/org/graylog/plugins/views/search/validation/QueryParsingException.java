package org.graylog.plugins.views.search.validation;

import org.apache.lucene.queryparser.classic.ParseException;

public class QueryParsingException extends Exception {
    public QueryParsingException(ParseException e) {
        super(e);
    }
}
