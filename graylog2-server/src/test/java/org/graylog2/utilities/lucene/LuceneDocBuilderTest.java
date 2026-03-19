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
import org.apache.lucene.index.IndexableField;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LuceneDocBuilderTest {

    private void assertTwoFieldsForQueryingAndSorting(Document doc, String fieldName) {
        List<IndexableField> fields = doc.getFields();
        assertThat(fields).hasSize(2);
        // Both fields should have the same name: one for searching/range queries, one for sorting
        assertThat(fields)
                .extracting(IndexableField::name)
                .containsOnly(fieldName);
    }

    @Test
    void testStringVal() {
        LuceneDocBuilder builder = new LuceneDocBuilder();
        Document doc = builder.stringVal("name", "test").getDoc();

        assertTwoFieldsForQueryingAndSorting(doc, "name");
        // Find the field with a string value (not all fields may have stringValue())
        assertThat(doc.getFields())
                .filteredOn(f -> f.stringValue() != null)
                .extracting(IndexableField::stringValue)
                .contains("test");
    }

    @Test
    void testStringValWithNull() {
        LuceneDocBuilder builder = new LuceneDocBuilder();
        Document doc = builder.stringVal("name", null).getDoc();

        assertThat(doc.getFields()).isEmpty();
    }

    @Test
    void testIntVal() {
        LuceneDocBuilder builder = new LuceneDocBuilder();
        Document doc = builder.intVal("age", 42).getDoc();

        assertTwoFieldsForQueryingAndSorting(doc, "age");
    }

    @Test
    void testIntValWithNull() {
        LuceneDocBuilder builder = new LuceneDocBuilder();
        Document doc = builder.intVal("age", null).getDoc();

        assertThat(doc.getFields()).isEmpty();
    }

    @Test
    void testLongVal() {
        LuceneDocBuilder builder = new LuceneDocBuilder();
        Document doc = builder.longVal("timestamp", 1234567890L).getDoc();

        assertTwoFieldsForQueryingAndSorting(doc, "timestamp");
    }

    @Test
    void testLongValWithNull() {
        LuceneDocBuilder builder = new LuceneDocBuilder();
        Document doc = builder.longVal("timestamp", null).getDoc();

        assertThat(doc.getFields()).isEmpty();
    }

    @Test
    void testDoubleVal() {
        LuceneDocBuilder builder = new LuceneDocBuilder();
        Document doc = builder.doubleVal("price", 99.99).getDoc();

        assertTwoFieldsForQueryingAndSorting(doc, "price");
    }

    @Test
    void testDoubleValWithNull() {
        LuceneDocBuilder builder = new LuceneDocBuilder();
        Document doc = builder.doubleVal("price", null).getDoc();

        assertThat(doc.getFields()).isEmpty();
    }

    @Test
    void testDateVal() {
        LuceneDocBuilder builder = new LuceneDocBuilder();
        Date date = new Date(1234567890000L);
        Document doc = builder.dateVal("created", date).getDoc();

        assertTwoFieldsForQueryingAndSorting(doc, "created");
    }

    @Test
    void testBoolValTrue() {
        LuceneDocBuilder builder = new LuceneDocBuilder();
        Document doc = builder.boolVal("active", true).getDoc();

        assertTwoFieldsForQueryingAndSorting(doc, "active");
    }

    @Test
    void testBoolValFalse() {
        LuceneDocBuilder builder = new LuceneDocBuilder();
        Document doc = builder.boolVal("active", false).getDoc();

        assertTwoFieldsForQueryingAndSorting(doc, "active");
    }

    @Test
    void testBoolValWithNull() {
        LuceneDocBuilder builder = new LuceneDocBuilder();
        Document doc = builder.boolVal("active", null).getDoc();

        assertThat(doc.getFields()).isEmpty();
    }

    @Test
    void testObjectVal() {
        LuceneDocBuilder builder = new LuceneDocBuilder();
        Object obj = 12345;
        Document doc = builder.objectVal("id", obj).getDoc();

        assertTwoFieldsForQueryingAndSorting(doc, "id");
        // Find the field with a string value (not all fields may have stringValue())
        assertThat(doc.getFields())
                .filteredOn(f -> f.stringValue() != null)
                .extracting(IndexableField::stringValue)
                .contains("12345");
    }

    @Test
    void testObjectValWithNull() {
        LuceneDocBuilder builder = new LuceneDocBuilder();
        Document doc = builder.objectVal("id", null).getDoc();

        assertThat(doc.getFields()).isEmpty();
    }

    @Test
    void testBuilderChaining() {
        LuceneDocBuilder builder = new LuceneDocBuilder();
        Document doc = builder
                .stringVal("name", "John Doe")
                .intVal("age", 30)
                .longVal("timestamp", 1234567890L)
                .doubleVal("score", 95.5)
                .boolVal("active", true)
                .getDoc();

        // 2 fields per value: one for searching/range queries, one for sorting
        assertThat(doc.getFields()).hasSize(10);
    }

    @Test
    void testMultipleFieldsWithSameName() {
        LuceneDocBuilder builder = new LuceneDocBuilder();
        Document doc = builder
                .stringVal("tag", "java")
                .stringVal("tag", "lucene")
                .getDoc();

        List<IndexableField> fields = doc.getFields();
        assertThat(fields).hasSize(4); // 2 fields per string value
        assertThat(fields.stream().filter(f -> f.name().equals("tag"))).hasSize(4);
    }

    @Test
    void testMixedNullAndNonNullValues() {
        LuceneDocBuilder builder = new LuceneDocBuilder();
        Document doc = builder
                .stringVal("name", "test")
                .stringVal("nullString", null)
                .intVal("age", 25)
                .intVal("nullInt", null)
                .getDoc();

        assertThat(doc.getFields()).hasSize(4); // Only non-null values
    }
}
