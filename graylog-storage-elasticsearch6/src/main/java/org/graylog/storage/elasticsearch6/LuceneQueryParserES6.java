package org.graylog.storage.elasticsearch6;

import org.graylog.plugins.views.search.engine.LuceneQueryParser;
import org.graylog.plugins.views.search.engine.LuceneQueryParsingException;
import org.graylog.shaded.elasticsearch5.org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.graylog.shaded.elasticsearch5.org.apache.lucene.index.Term;
import org.graylog.shaded.elasticsearch5.org.apache.lucene.queryparser.classic.ParseException;
import org.graylog.shaded.elasticsearch5.org.apache.lucene.queryparser.classic.QueryParser;
import org.graylog.shaded.elasticsearch5.org.apache.lucene.search.AutomatonQuery;
import org.graylog.shaded.elasticsearch5.org.apache.lucene.search.BooleanClause;
import org.graylog.shaded.elasticsearch5.org.apache.lucene.search.Query;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class LuceneQueryParserES6 implements LuceneQueryParser {

    private final QueryParser parser;

    public LuceneQueryParserES6() {
        this.parser = new QueryParser("f", new StandardAnalyzer());
    }

    @Override
    public Set<String> getFieldNames(String query) throws LuceneQueryParsingException {
        final Query parsed;
        try {
            parsed = parser.parse(query);
        } catch (ParseException e) {
            throw new LuceneQueryParsingException(e);
        }
        return Collections.emptySet();
    }
}
