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

import com.mongodb.BasicDBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Variable;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.favorites.FavoritesService;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.search.SearchQuery;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

public class ViewService extends PaginatedDbService<ViewDTO> {
    private static final String COLLECTION_NAME = "views";

    private final ClusterConfigService clusterConfigService;
    private final ViewRequirements.Factory viewRequirementsFactory;
    private final EntityOwnershipService entityOwnerShipService;
    private final ViewSummaryService viewSummaryService;
    private final MongoCollection<Document> collection;

    @Inject
    protected ViewService(MongoConnection mongoConnection,
                          MongoJackObjectMapperProvider mapper,
                          ClusterConfigService clusterConfigService,
                          ViewRequirements.Factory viewRequirementsFactory,
                          EntityOwnershipService entityOwnerShipService,
                          ViewSummaryService viewSummaryService) {
        super(mongoConnection, mapper, ViewDTO.class, COLLECTION_NAME);
        this.clusterConfigService = clusterConfigService;
        this.viewRequirementsFactory = viewRequirementsFactory;
        this.entityOwnerShipService = entityOwnerShipService;
        this.viewSummaryService = viewSummaryService;
        this.collection = mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME);
    }

    private PaginatedList<ViewDTO> searchPaginated(SearchUser searchUser,
                                                   SearchQuery query,
                                                   Predicate<ViewDTO> filter,
                                                   String order,
                                                   String sortField,
                                                   DBQuery.Query grandTotalQuery,
                                                   int page,
                                                   int perPage) {
        final PaginatedList<ViewDTO> viewsList = findPaginatedWithQueryFilterAndSortWithGrandTotal(searchUser, query, filter, getSortBuilder(order, sortField), grandTotalQuery, page, perPage);
        return viewsList.stream()
                .map(this::requirementsForView)
                .collect(Collectors.toCollection(() -> viewsList.grandTotal()
                        .map(grandTotal -> new PaginatedList<ViewDTO>(new ArrayList<>(viewsList.size()), viewsList.pagination().total(), page, perPage, grandTotal))
                        .orElseGet(() -> new PaginatedList<>(new ArrayList<>(viewsList.size()), viewsList.pagination().total(), page, perPage))));
    }

    protected PaginatedList<ViewDTO> findPaginatedWithQueryFilterAndSortWithGrandTotal(SearchUser searchUser,
                                                                                       SearchQuery dbQuery,
                                                                                   Predicate<ViewDTO> filter,
                                                                                   DBSort.SortBuilder sort,
                                                                                   DBQuery.Query grandTotalQuery,
                                                                                   int page,
                                                                                   int perPage) {
        var user = searchUser.getUser().getId();
        var query = dbQuery.toBson();
        final AggregateIterable<Document> result = collection.aggregate(List.of(
                        Aggregates.match(query),
                        Aggregates.lookup(
                                FavoritesService.COLLECTION_NAME,
                                List.of(
                                        new Variable<>("searchId", doc("$toString", "$_id")),
                                        new Variable<>("userId", user)
                                ),
                                List.of(Aggregates.unwind("$items"),
                                        Aggregates.match(
                                                doc("$expr", doc("$and", List.of(
                                                                doc("$eq", List.of("$items.id", "$$searchId")),
                                                                doc("$eq", List.of("$user_id", "$$userId"))
                                                        )
                                                ))),
                                        Aggregates.project(doc("_id", 1))
                                ),
                                "favorites"
                        ),
                        Aggregates.set(new Field<>("favorite", doc("$gt", List.of(doc("$size", "$favorites"), 0)))),
                        // replace with Aggregates.unset after switch to client 4.8
                        new BasicDBObject("$unset", "favorites"),
                        Aggregates.sort(sort)
                )
        );

        final long grandTotal = db.getCount(grandTotalQuery);

        final List<ViewDTO> views = StreamSupport.stream(result.spliterator(), false)
                .map(ViewDTO::fromDocument)
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

    private Document doc(String key, Object value) {
        return new Document(key, value);
    }

    public PaginatedList<ViewDTO> searchPaginated(SearchUser searchUser,
                                                  SearchQuery query,
                                                  Predicate<ViewDTO> filter,
                                                  String order,
                                                  String sortField,
                                                  int page,
                                                  int perPage) {
        return searchPaginated(searchUser, query, filter, order, sortField, DBQuery.empty(), page, perPage);
    }

    private PaginatedList<ViewDTO> searchPaginatedWithGrandTotal(DBQuery.Query query,
                                                                 Predicate<ViewDTO> filter,
                                                                 String order,
                                                                 String sortField,
                                                                 DBQuery.Query grandTotalQuery,
                                                                 int page,
                                                                 int perPage) {
        return findPaginatedWithQueryFilterAndSortWithGrandTotal(query, filter, getSortBuilder(order, sortField), grandTotalQuery, page, perPage);
    }

    public PaginatedList<ViewDTO> searchPaginatedByType(ViewDTO.Type type,
                                                        SearchQuery query,
                                                        Predicate<ViewDTO> filter,
                                                        String order,
                                                        String sortField,
                                                        int page,
                                                        int perPage) {
        checkNotNull(sortField);
        return searchPaginatedWithGrandTotal(
                DBQuery.and(
                        DBQuery.or(DBQuery.is(ViewDTO.FIELD_TYPE, type), DBQuery.notExists(ViewDTO.FIELD_TYPE)),
                        query.toDBQuery()
                ),
                filter,
                order,
                sortField,
                DBQuery.or(DBQuery.is(ViewDTO.FIELD_TYPE, type), DBQuery.notExists(ViewDTO.FIELD_TYPE)),
                page,
                perPage
        );
    }

    public PaginatedList<ViewSummaryDTO> searchSummariesPaginatedByType(final SearchUser searchUser,
                                                                        final ViewDTO.Type type,
                                                                        final SearchQuery query,
                                                                        final Predicate<ViewSummaryDTO> filter,
                                                                        final String order,
                                                                        final String sortField,
                                                                        final int page,
                                                                        final int perPage) {
        return viewSummaryService.searchPaginatedByType(searchUser, type, query, filter, order, sortField, page, perPage);
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
        return this.db.find(DBQuery.is(ViewDTO.FIELD_SEARCH_ID, searchId)).toArray()
                .stream()
                .map(this::requirementsForView)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<ViewDTO> get(String id) {
        return super.get(id).map(this::requirementsForView);
    }

    @Override
    public Stream<ViewDTO> streamAll() {
        return super.streamAll().map(this::requirementsForView);
    }

    @Override
    public Stream<ViewDTO> streamByIds(Set<String> idSet) {
        return super.streamByIds(idSet).map(this::requirementsForView);
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

    @Override
    public ViewDTO save(ViewDTO viewDTO) {
        try {
            final WriteResult<ViewDTO, ObjectId> save = db.insert(requirementsForView(viewDTO));
            return save.getSavedObject();
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Unable to save view, it already exists.");
        }
    }

    @Override
    public int delete(String id) {
        get(id).ifPresent(view -> {
            if (view.type().equals(ViewDTO.Type.DASHBOARD)) {
                entityOwnerShipService.unregisterDashboard(id);
            } else {
                entityOwnerShipService.unregisterSearch(id);
            }
        });
        return super.delete(id);
    }

    public ViewDTO update(ViewDTO viewDTO) {
        checkArgument(viewDTO.id() != null, "Id of view must not be null.");
        final ViewDTO viewWithRequirements = requirementsForView(viewDTO);
        db.updateById(new ObjectId(viewWithRequirements.id()), viewWithRequirements);
        return viewWithRequirements;
    }

    public ViewDTO requirementsForView(ViewDTO view) {
        return viewRequirementsFactory.create(view)
                .rebuildRequirements(ViewDTO::requires, (v, newRequirements) -> v.toBuilder().requires(newRequirements).build());
    }
}
