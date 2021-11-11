package org.graylog.plugins.views.search.engine;

import java.util.Set;

public interface LuceneQueryParser {
    Set<String> getFieldNames(final String query) throws LuceneQueryParsingException;
}
