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
package org.graylog2.database.slicing;

import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog.plugins.views.search.aggregations.MissingBucketConstants;
import org.graylog.plugins.views.search.permissions.EntityPermissions;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.aggregation.BucketAggregation;
import org.graylog2.database.aggregation.BucketAggregationResult;
import org.graylog2.database.aggregation.Grouping;
import org.graylog2.database.filtering.inmemory.SingleFilterParser;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.FilterOption;
import org.graylog2.rest.resources.entities.Slice;
import org.graylog2.rest.resources.entities.Slices;
import org.graylog2.rest.resources.system.contentpacks.titles.EntityTitleService;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntitiesTitleResponse;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityIdentifier;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityTitleRequest;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityTitleResponse;
import org.graylog2.search.SearchQueryField;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.graylog2.shared.utilities.StringUtils.f;
import static org.graylog2.users.UserImpl.LocalAdminUser.LOCAL_ADMIN_ID;

/**
 * Counts the entities of a list per value of one of its columns, for the slicing UI of entity tables.
 * <p>
 * Selecting a slice adds a {@code <column>:<value>} filter to the list, replacing the filters on that column. The
 * counts therefore use the list's query and its filters on all other columns, so each slice's count is the number of
 * entities the list shows after selecting it. A column holding several values per entity, like a list of assets,
 * counts each entity once per value.
 * <p>
 * Slices are titled by the column's filter options, by the related entities' titles if the column references another
 * collection, or else by their value. Related entities the user may not read, and ones that no longer exist, are
 * titled by their id, like in the list's cells. Every filter option is listed, also without entities. Entities without
 * a value are counted in a slice whose value is {@code null}.
 */
public class EntitySlicer {
    // Ordering by value too keeps slices with the same title, like two users of the same name, in a stable order.
    private static final Comparator<Slice> SLICE_ORDER = Comparator.comparing(Slice::count, Comparator.reverseOrder())
            .thenComparing(Slice::title, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Slice::value);

    private final MongoCollections mongoCollections;
    private final EntityTitleService entityTitleService;
    private final int maxSlices;

    @Inject
    public EntitySlicer(final MongoCollections mongoCollections, final EntityTitleService entityTitleService) {
        this(mongoCollections, entityTitleService, BucketAggregation.MAX_LIMIT);
    }

    EntitySlicer(final MongoCollections mongoCollections,
                 final EntityTitleService entityTitleService,
                 final int maxSlices) {
        this.mongoCollections = mongoCollections;
        this.entityTitleService = entityTitleService;
        this.maxSlices = maxSlices;
    }

    /**
     * @param entity           the list to slice
     * @param request          the column to slice by, and the list's current query and filters
     * @param scope            the entities the user may read; others are never counted
     * @param titlePermissions decides which related entities' titles the user may read
     * @throws BadRequestException if the column can't be sliced, or has more distinct values than can be listed
     */
    public Slices slices(final SliceableEntity entity,
                         final SliceRequest request,
                         final ReadScope scope,
                         final EntityPermissions titlePermissions) {
        final EntityAttribute attribute = sliceableAttribute(entity, request.sliceColumn());
        final Bson listFilter = entity.queryCreator().createDbQuery(filtersOnOtherColumns(request.filters(), attribute),
                request.query());
        // The filters match a plain attribute on its id, so slices group by it too.
        final BucketAggregationResult result = mongoCollections.aggregator(entity.collection())
                .buckets(BucketAggregation.of(Filters.and(scope.filter(), listFilter), Grouping.terms(attribute.id()))
                        .withLimit(maxSlices));
        if (result.isTruncated()) {
            throw new BadRequestException(f("Column <%s> has more than %d distinct values and can't be sliced",
                    attribute.id(), maxSlices));
        }

        final Map<String, Long> counts = new LinkedHashMap<>();
        result.buckets().forEach(bucket -> counts.put(String.valueOf(bucket.key()), bucket.count()));
        final Map<String, String> optionTitles = optionTitles(attribute);
        final Set<String> values = new LinkedHashSet<>(optionTitles.keySet());
        values.addAll(counts.keySet());
        final Map<String, String> relatedTitles = relatedTitles(attribute, values, titlePermissions);

        final Stream<Slice> valueSlices = values.stream()
                .map(value -> new Slice(value,
                        optionTitles.getOrDefault(value, relatedTitles.getOrDefault(value, value)),
                        Math.toIntExact(counts.getOrDefault(value, 0L))))
                .sorted(SLICE_ORDER);
        final Stream<Slice> missingSlice = result.missingCount() > 0
                ? Stream.of(new Slice(null, MissingBucketConstants.MISSING_BUCKET_NAME,
                        Math.toIntExact(result.missingCount())))
                : Stream.empty();
        return new Slices(Stream.concat(valueSlices, missingSlice).toList());
    }

    private static EntityAttribute sliceableAttribute(final SliceableEntity entity, final String column) {
        final EntityAttribute attribute = entity.sliceableAttribute(column)
                .orElseThrow(() -> new BadRequestException(f("Unknown slice column <%s>. Supported columns: %s",
                        column, entity.sliceableAttributeIds())));
        // A date's value isn't a filter for the entities on that date, and a custom filter can't be grouped by.
        if (attribute.type() == SearchQueryField.Type.DATE || attribute.bsonFilterCreator() != null) {
            throw new BadRequestException(f("Column <%s> can't be sliced", column));
        }
        return attribute;
    }

    private static List<String> filtersOnOtherColumns(final List<String> filters, final EntityAttribute attribute) {
        return filters.stream()
                .filter(filter -> !isFilterOn(filter, attribute))
                .toList();
    }

    // The filter parser accepts a column's title as well as its id.
    private static boolean isFilterOn(final String filter, final EntityAttribute attribute) {
        final String column = filter.split(SingleFilterParser.FIELD_AND_VALUE_SEPARATOR, 2)[0];
        return column.equals(attribute.id()) || column.equalsIgnoreCase(attribute.title());
    }

    private static Map<String, String> optionTitles(final EntityAttribute attribute) {
        final Set<FilterOption> options = attribute.filterOptions() == null ? Set.of() : attribute.filterOptions();
        return options.stream()
                .collect(Collectors.toMap(FilterOption::value, FilterOption::title, (first, second) -> first,
                        LinkedHashMap::new));
    }

    private Map<String, String> relatedTitles(final EntityAttribute attribute,
                                              final Set<String> values,
                                              final EntityPermissions titlePermissions) {
        if (attribute.relatedCollection() == null || values.isEmpty()) {
            return Map.of();
        }
        final boolean referencesObjectIds = attribute.relatedIdentifier() == null
                || "_id".equals(attribute.relatedIdentifier());
        final List<EntityIdentifier> entities = values.stream()
                // Looking up an invalid ObjectId would fail the whole request, and can't find anything anyway.
                // The title service knows the local admin, whose id isn't one.
                .filter(value -> !referencesObjectIds || ObjectId.isValid(value) || LOCAL_ADMIN_ID.equals(value))
                .map(value -> new EntityIdentifier(value,
                        attribute.relatedCollection(),
                        attribute.relatedIdentifier(),
                        referencesObjectIds ? null : SearchQueryField.Type.STRING.name(),
                        attribute.relatedDisplayFields(),
                        attribute.relatedDisplayTemplate()))
                .toList();
        final EntitiesTitleResponse response = entityTitleService.getTitles(new EntityTitleRequest(entities),
                titlePermissions);
        final Map<String, String> titles = new HashMap<>();
        for (final EntityTitleResponse title : response.entities()) {
            if (!response.notPermitted().contains(title.id()) && title.title() != null) {
                titles.put(title.id(), title.title());
            }
        }
        return titles;
    }
}
