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
package org.graylog2.database.aggregation;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Facet;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UnwindOptions;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.graylog2.database.aggregation.BucketAggregationResult.Bucket;
import org.graylog2.database.aggregation.grouping.DateHistogram;
import org.graylog2.database.aggregation.grouping.DateUnit;
import org.graylog2.database.aggregation.grouping.FieldSource;
import org.graylog2.database.aggregation.grouping.GroupedValueSource;
import org.graylog2.database.aggregation.grouping.Grouping;
import org.graylog2.database.aggregation.grouping.Ranges;
import org.graylog2.database.aggregation.grouping.RelatedSource;
import org.graylog2.database.aggregation.grouping.Terms;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Runs a bucket aggregation as one MongoDB pipeline and reads its result.
 * <p>
 * An aggregation runs in one pass: match, compute each document's distinct values into a temporary field, unwind
 * them, count per bucket key, then split the buckets into the missing bucket, the top buckets and the summed-up rest
 * with {@code $facet}.
 */
final class BucketQuery {
    private static final String FACET_BUCKETS = "buckets";
    private static final String FACET_MISSING = "missing";
    private static final String FACET_OTHER = "other";
    private static final String ID = "_id";
    private static final String COUNT = "count";
    private static final String BUCKET_COUNT = "bucket_count";

    private static final String VALUE = "__agg_value";
    private static final String RELATED = "__agg_related";
    private static final String REFERENCE = "__agg_reference";

    private BucketQuery() {}

    static BucketAggregationResult run(final MongoCollection<Document> collection,
                                       final BucketAggregationRequest request,
                                       final Duration timeout) {
        // $facet always returns exactly one document
        final Document facets = Objects.requireNonNull(collection.aggregate(pipeline(request))
                .allowDiskUse(true)
                .maxTime(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .first());
        final List<Bucket> buckets = facets.getList(FACET_BUCKETS, Document.class).stream()
                .map(bucket -> new Bucket(normalizeKey(bucket.get(ID)), count(bucket, COUNT)))
                .toList();
        final long missingCount = facets.getList(FACET_MISSING, Document.class).stream()
                .findFirst()
                .map(missing -> count(missing, COUNT))
                .orElse(0L);
        final Optional<Document> other = facets.getList(FACET_OTHER, Document.class).stream().findFirst();
        return new BucketAggregationResult(buckets,
                missingCount,
                other.map(rest -> count(rest, BUCKET_COUNT)).orElse(0L),
                other.map(rest -> count(rest, COUNT)).orElse(0L));
    }

    private static long count(final Document document, final String field) {
        return document.get(field, Number.class).longValue();
    }

    private static Object normalizeKey(final Object key) {
        return switch (key) {
            case Integer integer -> integer.longValue();
            case Date date -> date.toInstant();
            default -> key;
        };
    }

    private static List<Bson> pipeline(final BucketAggregationRequest request) {
        final var stages = new ArrayList<Bson>();
        stages.add(Aggregates.match(request.documentFilter()));
        stages.addAll(valueStages(request.grouping().source()));
        //unwinds for arrays, does nothing for single-value fields
        stages.add(Aggregates.unwind("$" + VALUE, new UnwindOptions().preserveNullAndEmptyArrays(true)));
        stages.add(Aggregates.group(bucketKey(request.grouping()), Accumulators.sum(COUNT, 1L)));

        final Bson hasKey = Aggregates.match(Filters.ne(ID, null));
        final Bson sort = Aggregates.sort(sort(request.order()));
        stages.add(Aggregates.facet(
                new Facet(FACET_BUCKETS, hasKey, sort, Aggregates.limit(request.limit())),
                new Facet(FACET_MISSING, Aggregates.match(Filters.eq(ID, null))),
                new Facet(FACET_OTHER, hasKey, sort, Aggregates.skip(request.limit()), Aggregates.group(null,
                        Accumulators.sum(BUCKET_COUNT, 1L),
                        Accumulators.sum(COUNT, "$" + COUNT)))));
        return stages;
    }

    private static List<Bson> valueStages(final GroupedValueSource source) {
        return switch (source) {
            case FieldSource field ->
                    List.of(Aggregates.set(new Field<>(VALUE, distinctValues("$" + field.path()))));
            case RelatedSource related -> relatedValueStages(related);
        };
    }

    private static List<Bson> relatedValueStages(final RelatedSource related) {
        final var stages = new ArrayList<Bson>();
        final var temporaryFields = new ArrayList<>(List.of(RELATED));
        var localKey = related.localKey();
        if (related.localKeyAsObjectId()) {
            stages.add(Aggregates.set(new Field<>(REFERENCE, toObjectIds("$" + related.localKey()))));
            localKey = REFERENCE;
            temporaryFields.add(REFERENCE);
        }

        // Without a reference, the lookup would match every related document lacking the related key.
        final var lookupStages = new ArrayList<>(List.of(Aggregates.match(Filters.ne(related.relatedKey(), null))));
        if (related.relatedFilter() != null) {
            lookupStages.add(Aggregates.match(related.relatedFilter()));
        }
        lookupStages.add(Aggregates.project(Projections.include(related.groupByField())));

        stages.add(lookup(related.relatedCollection(), localKey, related.relatedKey(), lookupStages, RELATED));
        stages.add(Aggregates.set(new Field<>(VALUE, distinctValues("$" + RELATED + "." + related.groupByField()))));
        stages.add(Aggregates.unset(temporaryFields));
        return stages;
    }

    /**
     * A {@code $lookup} that matches on fields and refines the matches with a pipeline. The driver has no builder
     * for this form, and encoding the stages here keeps the collection's codecs for values inside them.
     */
    private static Bson lookup(final String from,
                               final String localField,
                               final String foreignField,
                               final List<Bson> pipeline,
                               final String as) {
        return new Bson() {
            @Override
            public <TDocument> BsonDocument toBsonDocument(final Class<TDocument> documentClass,
                                                           final CodecRegistry codecRegistry) {
                final var stages = new BsonArray(pipeline.stream()
                        .map(stage -> (BsonValue) stage.toBsonDocument(documentClass, codecRegistry))
                        .toList());
                return new BsonDocument("$lookup", new BsonDocument("from", new BsonString(from))
                        .append("localField", new BsonString(localField))
                        .append("foreignField", new BsonString(foreignField))
                        .append("pipeline", stages)
                        .append("as", new BsonString(as)));
            }
        };
    }

    /**
     * The distinct non-null values of an expression. Paths through arrays of embedded documents can yield nested
     * arrays, so one level of nesting is flattened. A single value stays as it is.
     */
    private static Document distinctValues(final String expression) {
        final var flattened = new Document("$reduce", new Document("input", "$$values")
                .append("initialValue", List.of())
                .append("in", new Document("$setUnion", List.of("$$value", new Document("$cond",
                        new Document("if", new Document("$isArray", "$$this"))
                                .append("then", "$$this")
                                .append("else", List.of("$$this")))))));
        final var withoutNulls = new Document("$filter", new Document("input", flattened)
                .append("as", "element")
                .append("cond", new Document("$ne", Arrays.asList("$$element", null))));
        return new Document("$let", new Document("vars", new Document("values", expression))
                .append("in", new Document("$cond", new Document("if", new Document("$isArray", "$$values"))
                        .append("then", withoutNulls)
                        .append("else", "$$values"))));
    }

    private static Document toObjectIds(final String expression) {
        return new Document("$let", new Document("vars", new Document("reference", expression))
                .append("in", new Document("$cond", new Document("if", new Document("$isArray", "$$reference"))
                        .append("then", new Document("$map", new Document("input", "$$reference")
                                .append("in", toObjectId("$$this"))))
                        .append("else", toObjectId("$$reference")))));
    }

    private static Document toObjectId(final String expression) {
        return new Document("$convert", new Document("input", expression)
                .append("to", "objectId")
                .append("onError", null)
                .append("onNull", null));
    }

    private static Object bucketKey(final Grouping grouping) {
        final String value = "$" + VALUE;
        return switch (grouping) {
            case Terms terms -> value;
            case Ranges ranges -> rangeKey(ranges, value);
            case DateHistogram histogram -> dateHistogramKey(histogram, value);
        };
    }

    private static Document rangeKey(final Ranges ranges, final String value) {
        final Document hasRangeType = ranges.isTemporal() ? isDate(value) : new Document("$isNumber", value);
        final List<Document> branches = ranges.ranges().stream()
                .map(range -> {
                    final var conditions = new ArrayList<Object>(List.of(hasRangeType));
                    if (range.from() != null) {
                        conditions.add(new Document("$gte", List.of(value, bound(range.from()))));
                    }
                    if (range.to() != null) {
                        conditions.add(new Document("$lt", List.of(value, bound(range.to()))));
                    }
                    return new Document("case", new Document("$and", conditions))
                            .append("then", new Document("$literal", range.key()));
                })
                .toList();
        return new Document("$switch", new Document("branches", branches).append("default", null));
    }

    private static Object bound(final Object bound) {
        return bound instanceof Instant instant ? Date.from(instant) : bound;
    }

    private static Document dateHistogramKey(final DateHistogram histogram, final String value) {
        final var dateTrunc = new Document("date", value)
                .append("unit", histogram.unit().name().toLowerCase(Locale.ROOT))
                .append("binSize", histogram.binSize())
                .append("timezone", timezone(histogram.timezone()));
        if (histogram.unit() == DateUnit.WEEK) {
            dateTrunc.append("startOfWeek", "monday");
        }
        return new Document("$cond", new Document("if", isDate(value))
                .append("then", new Document("$dateTrunc", dateTrunc))
                .append("else", null));
    }

    private static String timezone(final ZoneId zone) {
        final ZoneId normalized = zone.normalized();
        if (normalized instanceof ZoneOffset offset) {
            // MongoDB doesn't accept "Z"
            return offset.getTotalSeconds() == 0 ? "+00:00" : offset.getId();
        }
        return normalized.getId();
    }

    private static Document isDate(final String expression) {
        return new Document("$eq", List.of(new Document("$type", expression), "date"));
    }

    private static Bson sort(final BucketOrder order) {
        return switch (order.sortBy()) {
            case KEY -> order.direction().toBsonSort(ID);
            case COUNT -> Sorts.orderBy(order.direction().toBsonSort(COUNT), Sorts.ascending(ID));
        };
    }
}
