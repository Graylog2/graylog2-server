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

import jakarta.annotation.Nonnull;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.search.SearchQueryField;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class LuceneInMemorySearchEngine<U extends InMemorySearchableEntity> implements InMemorySearchEngine<U> {

    private final String defaultField;
    private final List<EntityAttribute> attributes;
    private final Supplier<List<U>> datasource;
    private final Analyzer analyzer;
    private final StandardQueryParser queryParser;

    public LuceneInMemorySearchEngine(String defaultField, List<EntityAttribute> attributes, Supplier<List<U>> datasource) {
        this.defaultField = defaultField;
        this.attributes = attributes;
        this.datasource = datasource;
        this.analyzer = new StandardAnalyzer();
        this.queryParser = createQueryParser(analyzer);
    }

    @Override
    public PaginatedList<U> search(String queryString, String sortField, SortOrder order, int page, int perPage) throws IOException, QueryNodeException {
        try (Directory directory = new ByteBuffersDirectory();) {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            final List<U> entries = datasource.get();
            indexEntries(entries, directory, config);

            Query query = queryParser.parse(wrapEmptyQuery(queryString), defaultField);

            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                final Sort sort = createSort(sortField, order);

                final int start = page * perPage;
                final int end = start + perPage;
                final int offset = (page - 1) * perPage;

                TopDocs results = searcher.search(query, end, sort); // fetch enough docs
                final List<U> searchResults = extractSearchResults(results, searcher, offset, perPage, entries);

                final int totalCount = Math.toIntExact(results.totalHits.value);
                return new PaginatedList<>(searchResults, totalCount, page, perPage);
            }
        }
    }

    @Nonnull
    private StandardQueryParser createQueryParser(Analyzer analyzer) {
        StandardQueryParser parser = new StandardQueryParser(analyzer);
        parser.setAllowLeadingWildcard(true);
        parser.setPointsConfigMap(getStringPointsConfigMap());
        return parser;
    }

    private static <U extends InMemorySearchableEntity> @Nonnull List<U> extractSearchResults(TopDocs results, IndexSearcher searcher, int offset, int perPage, List<U> entries) {
        return Arrays.stream(results.scoreDocs)
                .skip(offset)
                .limit(perPage)
                .map(scoreDoc -> scoreDoc.doc)
                .map(scoreDocId -> storedDocument(searcher, scoreDocId))
                .map(doc -> Integer.parseInt(doc.get("_id"))) // this is my index in the original collection
                .map(entries::get)
                .toList();
    }

    private static Document storedDocument(IndexSearcher searcher, Integer docId) {
        try {
            return searcher.getIndexReader().storedFields().document(docId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void indexEntries(List<U> entities, Directory directory, IndexWriterConfig config) throws IOException {
        try (IndexWriter writer = new IndexWriter(directory, config)) {
            IntStream.range(0, entities.size())
                    .mapToObj(id -> toDoc(id, entities.get(id)))
                    .forEach(doc -> {
                        try {
                            writer.addDocument(doc);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    @Nonnull
    private Sort createSort(String sortField, SortOrder order) {
        final SortField.Type type = attributes.stream()
                .filter(a -> a.id().equals(sortField))
                .map(EntityAttribute::type)
                .map(this::toLuceneType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Couldn't detect sort type"));

        return new Sort(
                new SortField(sortField, type, order != SortOrder.ASCENDING)
        );
    }

    @Nonnull
    private Map<String, PointsConfig> getStringPointsConfigMap() {
        Map<String, PointsConfig> pointsConfig = new HashMap<>();
        attributes.stream()
                .filter(a -> a.type() == SearchQueryField.Type.INT)
                .forEach(a -> pointsConfig.put(a.id(), new PointsConfig(NumberFormat.getIntegerInstance(), Integer.class)));
        attributes.stream()
                .filter(a -> a.type() == SearchQueryField.Type.LONG)
                .forEach(a -> pointsConfig.put(a.id(), new PointsConfig(NumberFormat.getNumberInstance(), Long.class)));
        attributes.stream()
                .filter(a -> a.type() == SearchQueryField.Type.DOUBLE)
                .forEach(a -> pointsConfig.put(a.id(), new PointsConfig(NumberFormat.getNumberInstance(), Double.class)));
        return pointsConfig;
    }

    private static String wrapEmptyQuery(String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return "*";
        }
        return queryString;
    }

    private SortField.Type toLuceneType(SearchQueryField.Type type) {
        return switch (type) {
            case STRING -> SortField.Type.STRING;
            case DATE -> SortField.Type.INT;
            case DOUBLE -> SortField.Type.FLOAT;
            case INT -> SortField.Type.INT;
            case LONG -> SortField.Type.LONG;
            case OBJECT_ID -> SortField.Type.STRING;
            case BOOLEAN -> SortField.Type.INT;
        };
    }

    private Document toDoc(int id, U entity) {
        final LuceneDocBuilder builder = new LuceneDocBuilder();
        entity.buildLuceneDoc(builder);
        final Document doc = builder.getDoc();
        doc.add(new IntField("_id", id, Field.Store.YES));
        return doc;
    }
}
