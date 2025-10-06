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
import org.graylog.plugins.views.search.rest.ViewsRestPermissions;
import org.graylog.security.HasPermissions;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoEntity;
import org.graylog2.plugin.quickjump.rest.QuickJumpResponse;
import org.graylog2.shared.security.RestPermissions;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class QuickJumpService {
    private record Result(String source, String id, String title, int score) implements MongoEntity {}

    private final MongoCollection<Result> collection;
    private final Map<String, QuickJumpProvider> providers = Map.of(
            "views", QuickJumpProvider.create("views", (id, user) -> user.isPermitted(ViewsRestPermissions.VIEW_READ, id)),
            "streams", QuickJumpProvider.create("streams", (id, user) -> user.isPermitted(RestPermissions.STREAMS_READ, id)),
            "reports", QuickJumpProvider.create("reports", (id, user) -> user.isPermitted("reports:read", id))
    );

    @Inject
    public QuickJumpService(MongoCollections mongoCollections) {
        this.collection = mongoCollections.collection("dummy", Result.class);
    }

    public QuickJumpResponse search(String query, HasPermissions user) {
        final var limit = 100;
        // Precompute regex strings
        final var regexAnywhere = query;           // e.g. "foo"
        final var regexPrefix = "^" + Pattern.quote(query); // anchored, escapes special chars

        // Build the base branch (for the base collection "streams")
        final var baseBranch = branchPipeline(regexAnywhere, regexPrefix, "dummy");

        final var collections = Stream.of("streams", "views", "reports")
                .map(source -> new Document("$unionWith",
                        new Document("coll", source)
                                .append("pipeline", branchPipeline(regexAnywhere, regexPrefix, source))
                ))
                .toList();

        // Global sort + pagination + total via $facet
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

        // Optional: case-insensitive collation for tie-break string compares if any
        final var coll = Collation.builder().locale("en").collationStrength(CollationStrength.SECONDARY).build();

        final var results = StreamSupport.stream(collection.aggregate(pipeline).collation(coll).spliterator(), false).toList();
        return new QuickJumpResponse(results.stream()
                .filter(result -> checkPermission(result, user))
                .map(result -> new QuickJumpResponse.Result(result.source(), result.id(), result.title()))
                .toList()
        );
    }

    private boolean checkPermission(QuickJumpService.Result result, HasPermissions user) {
        final var provider = providers.get(result.source());
        return provider != null && provider.isPermitted(result.id(), user);
    }

    private static List<Bson> branchPipeline(String regexAnywhere, String regexPrefix, String sourceName) {
        // $match: anywhere, case-insensitive
        final var match = new Document("$match",
                new Document("title", new Document("$regex", regexAnywhere).append("$options", "i")));

        // score expression:
        // $switch with three branches: exact (3), prefix (2), anywhere (1)
        final var exactEq =
                new Document("$eq", Arrays.asList(
                        new Document("$toLower", "$title"),
                        new Document("$toLower", searchConst(regexAnywhere)) // reuse regex term as text; you can also pass an explicit lowercased plain term
                ));

        final var prefixMatch =
                new Document("$regexMatch",
                        new Document("input", "$title")
                                .append("regex", regexPrefix)
                                .append("options", "i"));

        final var anywhereMatch =
                new Document("$regexMatch",
                        new Document("input", "$title")
                                .append("regex", regexAnywhere)
                                .append("options", "i"));

        final var score =
                new Document("$switch", new Document("branches", Arrays.asList(
                        new Document("case", exactEq).append("then", 3),
                        new Document("case", prefixMatch).append("then", 2),
                        new Document("case", anywhereMatch).append("then", 1)
                )).append("default", 0));

        // $project: common shape
        final var project = new Document("$project", new Document()
                .append("_id", 1)
                .append("title", 1)
                .append("createdAt", 1)
                .append("source", new Document("$literal", sourceName))
                .append("score", score)
        );

        return Arrays.asList(match, project);
    }

    // Helper: if you want to pass a constant string and ensure it's treated as a value (not a field path)
    private static Object searchConst(String value) {
        // You can just return the string; wrapping is here to make intent explicit.
        return value;
    }
}
