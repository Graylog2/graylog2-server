package org.graylog.plugins.views.search.validation;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.CharStream;
import org.apache.lucene.queryparser.classic.FastCharStream;
import org.apache.lucene.queryparser.classic.QueryParserTokenManager;
import org.apache.lucene.queryparser.classic.Token;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

public class CollectingQueryParserTokenManager extends QueryParserTokenManager {

    private final List<Token> tokens = new LinkedList<>();

    public CollectingQueryParserTokenManager(String f, Analyzer a) {
        super(new FastCharStream(new StringReader("")));
    }

    @Override
    public void ReInit(CharStream stream) {
        this.tokens.clear();
        super.ReInit(stream);
    }

    @Override
    public Token getNextToken() {
        final Token token = super.getNextToken();
        this.tokens.add(token);
        return token;
    }

    public List<Token> getTokens() {
        return tokens;
    }
}
