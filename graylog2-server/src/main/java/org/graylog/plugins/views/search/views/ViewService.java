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
package org.graylog.plugins.views.search.views;

import com.mongodb.DuplicateKeyException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import jakarta.inject.Inject;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.indices.MongoDbIndexTools;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.search.SearchQuery;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

public class ViewService implements ViewUtils<ViewDTO> {
    private static final String COLLECTION_NAME = "views";

    private final ClusterConfigService clusterConfigService;
    private final ViewRequirements.Factory viewRequirementsFactory;
    private final EntityOwnershipService entityOwnerShipService;
    private final ViewSummaryService viewSummaryService;
    private final MongoCollection<ViewDTO> collection;
    private final MongoPaginationHelper<ViewDTO> pagination;
    private final MongoUtils<ViewDTO> mongoUtils;

    @Inject
    protected ViewService(ClusterConfigService clusterConfigService,
                          ViewRequirements.Factory viewRequirementsFactory,
                          EntityOwnershipService entityOwnerShipService,
                          ViewSummaryService viewSummaryService,
                          MongoCollections mongoCollections) {
        this.clusterConfigService = clusterConfigService;
        this.viewRequirementsFactory = viewRequirementsFactory;
        this.entityOwnerShipService = entityOwnerShipService;
        this.viewSummaryService = viewSummaryService;
        this.collection = mongoCollections.collection(COLLECTION_NAME, ViewDTO.class);
        this.pagination = mongoCollections.paginationHelper(this.collection);
        this.mongoUtils = mongoCollections.utils(collection);

        new MongoDbIndexTools<>(collection).prepareIndices(ViewDTO.FIELD_ID, ViewDTO.SORT_FIELDS, ViewDTO.STRING_SORT_FIELDS);
    }

    private PaginatedList<ViewDTO> searchPaginated(SearchUser searchUser,
                                                   SearchQuery query,
                                                   Predicate<ViewDTO> filter,
                                                   SortOrder order,
                                                   String sortField,
                                                   Bson grandTotalQuery,
                                                   int page,
                                                   int perPage) {
        final PaginatedList<ViewDTO> viewsList = findPaginatedWithQueryFilterAndSortWithGrandTotal(searchUser, query, filter,
                Sorts.orderBy(order.toBsonSort(sortField), order.toBsonSort(ViewDTO.SECONDARY_SORT)),
                grandTotalQuery, page, perPage);
        return viewsList.withList(viewsList.delegate().stream()
                .map(this::requirementsForView)
                .toList());
    }

    public Optional<ViewDTO> get(final SearchUser searchUser, final String id) {
        return findViews(searchUser, Filters.eq("_id", new ObjectId(id)), Sorts.ascending("_id"))
                .findFirst();
    }

    protected PaginatedList<ViewDTO> findPaginatedWithQueryFilterAndSortWithGrandTotal(SearchUser searchUser,
                                                                                       SearchQuery dbQuery,
                                                                                       Predicate<ViewDTO> filter,
                                                                                       Bson sort,
                                                                                       Bson grandTotalQuery,
                                                                                       int page,
                                                                                       int perPage) {
        var grandTotal = collection.countDocuments(grandTotalQuery);

        var views = findViews(searchUser, dbQuery.toBson(), sort)
                .filter(filter)
                .toList();

        final List<ViewDTO> paginatedStreams = perPage > 0
                ? views.stream()
                .skip((long) perPage * Math.max(0, page - 1))
                .limit(perPage)
                .toList()
                : views;

        return new PaginatedList<>(paginatedStreams, views.size(), page, perPage, grandTotal);
    }

    public PaginatedList<ViewDTO> searchPaginated(SearchUser searchUser,
                                                  SearchQuery query,
                                                  Predicate<ViewDTO> filter,
                                                  SortOrder order,
                                                  String sortField,
                                                  int page,
                                                  int perPage) {
        return searchPaginated(searchUser, query, filter, order, sortField, new BsonDocument(), page, perPage);
    }

    private PaginatedList<ViewDTO> searchPaginatedWithGrandTotal(Bson query,
                                                                 Predicate<ViewDTO> filter,
                                                                 SortOrder order,
                                                                 String sortField,
                                                                 Bson grandTotalQuery,
                                                                 int page,
                                                                 int perPage) {
        return pagination.perPage(perPage)
                .sort(order.toBsonSort(sortField))
                .filter(query)
                .includeGrandTotal(true)
                .grandTotalFilter(grandTotalQuery)
                .page(page, filter);
    }

    public PaginatedList<ViewDTO> searchPaginatedByType(ViewDTO.Type type,
                                                        SearchQuery query,
                                                        Predicate<ViewDTO> filter,
                                                        SortOrder order,
                                                        String sortField,
                                                        int page,
                                                        int perPage) {
        checkNotNull(sortField);
        return searchPaginatedWithGrandTotal(
                Filters.and(
                        Filters.or(Filters.eq(ViewDTO.FIELD_TYPE, type), Filters.not(Filters.exists(ViewDTO.FIELD_TYPE))),
                        query.toBson()
                ),
                filter,
                order,
                sortField,
                Filters.or(Filters.eq(ViewDTO.FIELD_TYPE, type), Filters.not(Filters.exists(ViewDTO.FIELD_TYPE))),
                page,
                perPage
        );
    }

    public PaginatedList<ViewSummaryDTO> searchSummariesPaginatedByType(final SearchUser searchUser,
                                                                        final ViewDTO.Type type,
                                                                        final Bson dbQuery, //query executed on DB level
                                                                        final Predicate<ViewSummaryDTO> predicate, //predicate executed on code level, AFTER data is fetched
                                                                        final SortOrder order,
                                                                        final String sortField,
                                                                        final int page,
                                                                        final int perPage) {
        return viewSummaryService.searchPaginatedByType(searchUser, type, dbQuery, predicate, order, sortField, page, perPage);
    }

    public void saveDefault(ViewDTO dto) {
        if (isNullOrEmpty(dto.id())) {
            throw new IllegalArgumentException("ViewDTO needs an ID to be configured as default view");
        }
        clusterConfigService.write(ViewClusterConfig.builder()
                .defaultViewId(dto.id())
                .build());
    }

    public Optional<ViewDTO> getDefault() {
        return Optional.ofNullable(clusterConfigService.get(ViewClusterConfig.class))
                .flatMap(config -> get(config.defaultViewId()));
    }

    public Collection<ViewDTO> forSearch(String searchId) {
        return MongoUtils.stream(this.collection.find(Filters.eq(ViewDTO.FIELD_SEARCH_ID, searchId)))
                .map(this::requirementsForView)
                .collect(Collectors.toSet());
    }

    public Optional<ViewDTO> get(String id) {
        return mongoUtils.getById(id).map(this::requirementsForView);
    }

    public Stream<ViewDTO> streamAll() {
        return MongoUtils.stream(collection.find()).map(this::requirementsForView);
    }

    public Stream<ViewDTO> streamByIds(Set<String> idSet) {
        return MongoUtils.stream(collection.find(MongoUtils.stringIdsIn(idSet))).map(this::requirementsForView);
    }

    public ViewDTO saveWithOwner(ViewDTO viewDTO, User user) {
        final ViewDTO savedObject = save(viewDTO);
        if (viewDTO.type().equals(ViewDTO.Type.DASHBOARD)) {
            entityOwnerShipService.registerNewDashboard(savedObject.id(), user);
        } else {
            entityOwnerShipService.registerNewSearch(savedObject.id(), user);
        }
        return savedObject;
    }

    public ViewDTO save(ViewDTO viewDTO) {
        try {
            final var save = collection.insertOne(requirementsForView(viewDTO));
            return mongoUtils.getById(MongoUtils.insertedId(save)).orElseThrow(() -> new IllegalStateException("Unable to retrieve saved View!"));
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Unable to save view, it already exists.");
        }
    }

    public void delete(String id) {
        get(id).ifPresent(view -> {
            if (view.type().equals(ViewDTO.Type.DASHBOARD)) {
                entityOwnerShipService.unregisterDashboard(id);
            } else {
                entityOwnerShipService.unregisterSearch(id);
            }
        });
        mongoUtils.deleteById(id);
    }

    public ViewDTO update(ViewDTO viewDTO) {
        checkArgument(viewDTO.id() != null, "Id of view must not be null.");
        final ViewDTO viewWithRequirements = requirementsForView(viewDTO).toBuilder().lastUpdatedAt(DateTime.now(DateTimeZone.UTC)).build();
        collection.replaceOne(MongoUtils.idEq(viewWithRequirements.id()), viewWithRequirements);
        return viewWithRequirements;
    }

    public ViewDTO requirementsForView(ViewDTO view) {
        return viewRequirementsFactory.create(view)
                .rebuildRequirements(ViewDTO::requires, (v, newRequirements) -> v.toBuilder().requires(newRequirements).build());
    }

    @Override
    public MongoCollection<ViewDTO> collection() {
        return collection;
    }
}
