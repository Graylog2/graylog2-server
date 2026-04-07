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
package org.graylog.plugins.views.search.validation;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * The default BooleanQuery from lucene uses Multiset implementations in the visit method. These lose the reference
 * to the actual query instance and aggregate queries if they are the same. This breaks our terms detection in
 * validation. So we have to provide this alternative class that iterates bool clauses with correct references
 */
public class FixedBooleanQuery extends Query implements Iterable<BooleanClause> {

    private final BooleanQuery delegate;

    public FixedBooleanQuery(BooleanQuery delegate) {
        this.delegate = delegate;
    }

    @Override
    public String toString(String field) {
        return delegate.toString(field);
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public Iterator<BooleanClause> iterator() {
        return delegate.iterator();
    }

    @Override
    public void forEach(Consumer<? super BooleanClause> consumer) {
        delegate.forEach(consumer);
    }

    @Override
    public Spliterator<BooleanClause> spliterator() {
        return delegate.spliterator();
    }

    /**
     * This is the only purpose of this class - provide a simpler visit method that preserves the query reference
     * to the underlying object. That reference is then used to obtain related query terms during validation.
     */
    @Override
    public void visit(QueryVisitor visitor) {
        delegate.clauses().forEach(c -> {
            final QueryVisitor sub = visitor.getSubVisitor(c.getOccur(), delegate);
            c.getQuery().visit(sub);
        });
    }
}
