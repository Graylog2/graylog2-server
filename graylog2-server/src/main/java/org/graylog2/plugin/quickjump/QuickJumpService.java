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
package org.graylog2.plugin.quickjump;

import com.google.common.collect.ImmutableList;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.security.HasPermissions;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoEntity;
import org.graylog2.plugin.quickjump.rest.QuickJumpResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.graylog2.plugin.quickjump.QuickJumpConstants.DEFAULT_COLLATION;
import static org.graylog2.plugin.quickjump.QuickJumpConstants.DEFAULT_FIELDS;
import static org.graylog2.plugin.quickjump.QuickJumpConstants.DUMMY_COLLECTION;

public class QuickJumpService {
    private record Result(String source, String type, String id, String title, int score) implements MongoEntity {}

    private final MongoCollection<Result> collection;
    private final Map<String, QuickJumpProvider> providers;

    @Inject
    public QuickJumpService(MongoCollections mongoCollections, Set<QuickJumpProvider> providers) {
        this.collection = mongoCollections.collection(DUMMY_COLLECTION, Result.class);
        this.providers = providers.stream().collect(Collectors.toMap(QuickJumpProvider::type, Function.identity()));
    }

    public QuickJumpResponse search(String query, int limit, HasPermissions user) {
        final var baseBranch = branchPipeline(query, DEFAULT_FIELDS, DUMMY_COLLECTION, new Document("$literal", DUMMY_COLLECTION));

        final var collections = providers.entrySet().stream()
                .map(entry -> {
                    final var source = entry.getKey();
                    final var provider = entry.getValue();
                    return new Document("$unionWith",
                            new Document("coll", provider.collectionName())
                                .append("pipeline", branchPipeline(query, provider.fieldsToSearch(), source, provider.typeField()))
                    );
                })
                .toList();

        final var sort = new Document("$sort",
                new Document("score", -1)
                        .append("createdAt", -1)
                        .append("_id", 1));

        final var pipeline = ImmutableList.<Bson>builder()
                .addAll(baseBranch)
                .addAll(collections)
                .add(sort)
                .add(new Document("$limit", limit))
                .build();

        final var coll = Collation.builder().locale(DEFAULT_COLLATION).collationStrength(CollationStrength.SECONDARY).build();

        final var results = StreamSupport.stream(collection.aggregate(pipeline).collation(coll).spliterator(), false).toList();
        return new QuickJumpResponse(results.stream()
                .filter(result -> checkPermission(result, user))
                .map(result -> new QuickJumpResponse.Result(result.type(), result.id(), result.title()))
                .toList()
        );
    }

    private boolean checkPermission(QuickJumpService.Result result, HasPermissions user) {
        final var provider = providers.get(result.source());
        return provider != null && provider.isPermitted(result.id(), user);
    }

    private static List<Bson> branchPipeline(String query, List<String> fields, String sourceName, Bson typeField) {
        final var match = new Document("$match", new Document("$or",
                fields.stream().map(field -> new Document(field, new Document("$regex", query).append("$options", "i"))).toList()
        ));

        final var fieldMatchers = IntStream.range(0, fields.size())
                .boxed()
                .flatMap(idx -> fieldMatchers(query, fields.reversed().get(idx), idx * 10))
                .toList();

        final var score = new Document("$switch", new Document("branches", fieldMatchers).append("default", 0));

        final var project = new Document("$project", new Document()
                .append("_id", 1)
                .append("title", 1)
                .append("createdAt", 1)
                .append("source", sourceName)
                .append("type", typeField)
                .append("score", score)
        );

        return Arrays.asList(match, project);
    }

    private static Stream<Document> fieldMatchers(String query, String fieldName, int scoreBase) {
        final var regexPrefix = "^" + Pattern.quote(query);
        final var fieldRef = "$" + fieldName;
        final var exactEq = new Document("$eq", Arrays.asList(
                        new Document("$toLower", fieldRef),
                        new Document("$toLower", searchConst(query)) // reuse regex term as text; you can also pass an explicit lowercased plain term
                ));

        final var prefixMatch = new Document("$regexMatch",
                        new Document("input", fieldRef)
                                .append("regex", regexPrefix)
                                .append("options", "i"));

        final var anywhereMatch = new Document("$regexMatch",
                        new Document("input", fieldRef)
                                .append("regex", query)
                                .append("options", "i"));
        return Stream.of(
                new Document("case", exactEq).append("then", scoreBase + 3),
                new Document("case", prefixMatch).append("then", scoreBase + 2),
                new Document("case", anywhereMatch).append("then", scoreBase + 1)
        );
    }

    private static Object searchConst(String value) {
        return value;
    }
}
