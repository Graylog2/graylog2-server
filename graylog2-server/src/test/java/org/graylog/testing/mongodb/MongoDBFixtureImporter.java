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
package org.graylog.testing.mongodb;

import com.google.common.io.Resources;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Imports data into a MongoDB instance based on JSON files.
 * <p>
 * The JSON files can look either like this:
 * <pre>{@code
 *  {
 *      "<collection-name>": [
 *          {
 *              "_id": {"$oid": "54e3deadbeefdeadbeefaffe"},
 *              "field_a": "content1",
 *              "created_at": {"$date": "2019-01-01T00:00:00.000Z"}
 *          }
 *      ]
 *  }
 * }</pre>
 * <p>
 * Or alternatively like this:
 * <pre>{@code
 * {
 *     "<collection-name>": {
 *         "data": [
 *             {
 *                 "_id": {"$oid": "54e3deadbeefdeadbeefaffe"},
 *                 "field_a": "content1",
 *                 "created_at": {"$date": "2019-01-01T00:00:00.000Z"}
 *             }
 *         ],
 *         "indexes": [
 *             {
 *                 "index": {"field_a": 1},
 *                 "options": {"unique": true}
 *             }
 *         ]
 *     }
 * }
 * }</pre>
 */
class MongoDBFixtureImporter {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDBFixtureImporter.class);
    private static final String FIELD_DATA = "data";
    private static final String FIELD_INDEX = "index";
    private static final String FIELD_INDEXES = "indexes";
    private static final String FIELD_INDEX_OPTIONS = "options";

    private final List<URL> resources;

    /**
     * Import the given resources using the given database connection.
     *
     * @param fixtureResources resources to import
     * @param contextClass     context class to lookup the resources in
     */
    MongoDBFixtureImporter(String[] fixtureResources, Class<?> contextClass) {
        this.resources = buildFixtureResources(fixtureResources, contextClass);
    }

    MongoDBFixtureImporter(List<URL> fixtureResources) {
        this.resources = fixtureResources;
    }

    void importResources(MongoDatabase database) {
        resources.forEach(resource -> importResource(resource, database));
    }

    private static List<URL> buildFixtureResources(String[] resourceNames, Class<?> contextClass) {
        return Arrays.stream(resourceNames)
                .map(resourceName -> toResource(resourceName, contextClass))
                .collect(Collectors.toList());
    }

    private static URL toResource(final String resourceName, final Class<?> contextClass) {
        if (! Paths.get(resourceName).isAbsolute()) {
            try {
                return Resources.getResource(contextClass, resourceName);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Resources.getResource(resourceName);
    }

    private void importResource(URL resource, MongoDatabase database) {
        LOG.debug("Importing fixture resource: {}", resource);
        try {
            importData(database, Resources.toString(resource, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void importData(MongoDatabase database, String jsonData) {
        final Document data = Document.parse(jsonData);

        for (final String collectionName : data.keySet()) {
            final Object document = data.get(collectionName, Object.class);

            if (isListOfDocuments(document)) {
                importDocuments(database, collectionName, data.get(collectionName, List.class));
            } else {
                final Document doc = (Document) document;
                importIndexes(database, collectionName, doc);
                importDocuments(database, collectionName, doc.get(FIELD_DATA, List.class));
            }
        }
    }

    private boolean isListOfDocuments(Object object) {
        return List.class.isAssignableFrom(object.getClass());
    }

    private void importDocuments(MongoDatabase database, String collectionName, List<Document> dataObjects) {
        final MongoCollection<Document> collection = database.getCollection(collectionName);

        for (final Document dataObject : dataObjects) {
            collection.insertOne(dataObject);
        }
    }

    @SuppressWarnings("unchecked")
    private void importIndexes(MongoDatabase database, String collectionName, Document doc) {
        if (!doc.containsKey(FIELD_INDEXES)) {
            return;
        }

        final MongoCollection<Document> collection = database.getCollection(collectionName);
        final List<Document> indexes = doc.get(FIELD_INDEXES, List.class);

        for (final Document indexDoc : indexes) {
            final Document indexFields = indexDoc.get(FIELD_INDEX, Document.class);

            if (indexDoc.containsKey(FIELD_INDEX_OPTIONS)) {
                collection.createIndex(indexFields, createIndexOptions(indexDoc.get(FIELD_INDEX_OPTIONS, Document.class)));
            } else {
                collection.createIndex(indexFields);
            }
        }
    }

    private IndexOptions createIndexOptions(Document indexOptionsDoc) {
        final IndexOptions indexOptions = new IndexOptions();

        if (indexOptionsDoc.containsKey("background")) {
            indexOptions.background(indexOptionsDoc.getBoolean("background"));
        }

        if (indexOptionsDoc.containsKey("unique")) {
            indexOptions.unique(indexOptionsDoc.getBoolean("unique"));
        }

        if (indexOptionsDoc.containsKey("name")) {
            indexOptions.name(indexOptionsDoc.getString("name"));
        }

        if (indexOptionsDoc.containsKey("sparse")) {
            indexOptions.sparse(indexOptionsDoc.getBoolean("sparse"));
        }

        if (indexOptionsDoc.containsKey("expireAfterSeconds")) {
            indexOptions.expireAfter(indexOptionsDoc.getLong("expireAfterSeconds"), TimeUnit.SECONDS);
        }

        if (indexOptionsDoc.containsKey("version")) {
            indexOptions.version(indexOptionsDoc.getInteger("version"));
        }

        if (indexOptionsDoc.containsKey("weights")) {
            indexOptions.weights(indexOptionsDoc.get("weights", Bson.class));
        }

        if (indexOptionsDoc.containsKey("defaultLanguage")) {
            indexOptions.defaultLanguage(indexOptionsDoc.getString("defaultLanguage"));
        }

        if (indexOptionsDoc.containsKey("languageOverride")) {
            indexOptions.languageOverride(indexOptionsDoc.getString("languageOverride"));
        }

        if (indexOptionsDoc.containsKey("textVersion")) {
            indexOptions.textVersion(indexOptionsDoc.getInteger("textVersion"));
        }

        if (indexOptionsDoc.containsKey("sphereVersion")) {
            indexOptions.sphereVersion(indexOptionsDoc.getInteger("sphereVersion"));
        }

        if (indexOptionsDoc.containsKey("bits")) {
            indexOptions.bits(indexOptionsDoc.getInteger("bits"));
        }

        if (indexOptionsDoc.containsKey("min")) {
            indexOptions.min(indexOptionsDoc.getDouble("min"));
        }

        if (indexOptionsDoc.containsKey("max")) {
            indexOptions.max(indexOptionsDoc.getDouble("max"));
        }

        if (indexOptionsDoc.containsKey("bucketSize")) {
            indexOptions.bucketSize(indexOptionsDoc.getDouble("bucketSize"));
        }

        if (indexOptionsDoc.containsKey("storageEngine")) {
            indexOptions.storageEngine(indexOptionsDoc.get("storageEngine", Bson.class));
        }

        if (indexOptionsDoc.containsKey("partialFilterExpression")) {
            indexOptions.partialFilterExpression(indexOptionsDoc.get("partialFilterExpression", Bson.class));
        }

        return indexOptions;
    }
}
