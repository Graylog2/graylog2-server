/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.views.search.engine;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.AutomatonQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;

import java.util.HashSet;
import java.util.Set;

public class LuceneQueryParser {
    private final org.apache.lucene.queryparser.classic.QueryParser parser;

    public LuceneQueryParser() {
        this.parser = new org.apache.lucene.queryparser.classic.QueryParser("f", new StandardAnalyzer());
    }

    public Set<String> getFieldNames(final String query) throws LuceneQueryParsingException {
        final Query parsed;
        try {
            parsed = parser.parse(query);
        } catch (ParseException e) {
            throw new LuceneQueryParsingException(e);
        }
        final Set<String> fields = new HashSet<>();
        parsed.visit(new QueryVisitor() {
            @Override
            public void consumeTerms(Query query, Term... terms) {
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
