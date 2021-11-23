package org.graylog.plugins.views.search.validation;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.Token;

import java.util.List;

public class TermCollectingQueryParser extends QueryParser {

    public TermCollectingQueryParser(String defaultFieldName, Analyzer analyzer) {
        super(new CollectingQueryParserTokenManager(defaultFieldName, analyzer));
        init(defaultFieldName, analyzer);
    }

    public List<Token> getTokens() {
        return ((CollectingQueryParserTokenManager) super.token_source).getTokens();
    }
}
