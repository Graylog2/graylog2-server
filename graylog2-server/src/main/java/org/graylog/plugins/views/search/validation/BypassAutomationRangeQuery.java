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

import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.util.BytesRef;

/**
 * This TermRangeQuery bypasses Automation and its generated states when it comes to the visitor pattern.
 * It simply passes itself to the {@link TermCollectingQueryVisitor#visitLeaf(Query)}. The visitor knows
 * how to handle the range for our validation purposes.
 */
public class BypassAutomationRangeQuery extends TermRangeQuery {

    public BypassAutomationRangeQuery(String field, BytesRef lowerTerm, BytesRef upperTerm, boolean includeLower, boolean includeUpper, RewriteMethod rewriteMethod) {
        super(field, lowerTerm, upperTerm, includeLower, includeUpper, rewriteMethod);
    }

    @Override
    public void visit(QueryVisitor visitor) {
        if (visitor.acceptField(field)) {
           visitor.visitLeaf(this);
        }
    }
}
