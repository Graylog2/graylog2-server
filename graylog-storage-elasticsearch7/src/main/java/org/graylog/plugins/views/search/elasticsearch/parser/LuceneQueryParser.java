package org.graylog.plugins.views.search.elasticsearch.parser;

import org.graylog.shaded.elasticsearch7.org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.graylog.shaded.elasticsearch7.org.apache.lucene.index.Term;
import org.graylog.shaded.elasticsearch7.org.apache.lucene.queryparser.classic.ParseException;
import org.graylog.shaded.elasticsearch7.org.apache.lucene.queryparser.classic.QueryParser;
import org.graylog.shaded.elasticsearch7.org.apache.lucene.search.AutomatonQuery;
import org.graylog.shaded.elasticsearch7.org.apache.lucene.search.BooleanClause;
import org.graylog.shaded.elasticsearch7.org.apache.lucene.search.Query;
import org.graylog.shaded.elasticsearch7.org.apache.lucene.search.QueryVisitor;

import java.util.HashSet;
import java.util.Set;

public class LuceneQueryParser {

    private final QueryParser parser;

    public LuceneQueryParser() {
        this.parser = new QueryParser("f", new StandardAnalyzer());
    }

    public Set<String> getFieldNames(final String query) throws ParseException {
        final Query parsed = parser.parse(query);
        final Set<String> fields = new HashSet<>();
        parsed.visit(new QueryVisitor() {
            @Override
            public void consumeTerms(org.graylog.shaded.elasticsearch7.org.apache.lucene.search.Query query, Term... terms) {
                super.consumeTerms(query, terms);
                for (Term t : terms) {
                    final String field = t.field();
                    if (field.equals("_exists_")) {
                        fields.add(t.text());
                    } else {
                        fields.add(field);
                    }
                }
            }

            @Override
            public void visitLeaf(Query query) {
                if (query instanceof AutomatonQuery) {
                    final String field = ((AutomatonQuery) query).getField();
                    fields.add(field);
                }
            }

            @Override
            public QueryVisitor getSubVisitor(BooleanClause.Occur occur, Query parent) {
                // the default implementation ignores MUST_NOT clauses, we want to collect all, even MUST_NOT
                return this;
            }
        });
        return fields;
    }
}
