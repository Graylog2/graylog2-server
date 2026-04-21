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
package org.graylog2.utilities.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleDocValuesField;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.util.BytesRef;

import java.util.Date;

public class LuceneDocBuilder {
    private final Document doc = new Document();


    public LuceneDocBuilder stringVal(String key, String value) {
        if (value != null) {
            doc.add(new TextField(key, value, Field.Store.NO));
            doc.add(new SortedDocValuesField(key, new BytesRef(value)));
        }
        return this;
    }

    public LuceneDocBuilder dateVal(String key, Date value) {
        return longVal(key, value.getTime());
    }

    public LuceneDocBuilder doubleVal(String key, Double value) {
        if (value != null) {
            doc.add(new DoublePoint(key, value));
            doc.add(new DoubleDocValuesField(key, value));
        }
        return this;
    }

    public LuceneDocBuilder intVal(String key, Integer value) {
        if (value != null) {
            doc.add(new IntPoint(key, value));
            doc.add(new NumericDocValuesField(key, value));
        }
        return this;
    }

    public LuceneDocBuilder longVal(String key, Long value) {
        if (value != null) {
            doc.add(new LongPoint(key, value));
            doc.add(new NumericDocValuesField(key, value));
        }
        return this;
    }

    public LuceneDocBuilder objectVal(String key, Object value) {
        return stringVal(key, value != null ? value.toString() : null);
    }

    public LuceneDocBuilder boolVal(String key, Boolean value) {
        if (value != null) {
            return intVal(key, value ? 1 : 0);
        }
        return this;
    }

    public Document getDoc() {
        return doc;
    }
}
