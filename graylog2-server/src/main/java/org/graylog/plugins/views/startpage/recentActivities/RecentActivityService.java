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
package org.graylog.plugins.views.startpage.recentActivities;

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog.grn.GRNTypes;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.security.Capability;
import org.graylog.security.CapabilityRegistry;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog.security.PermissionAndRoleResolver;
import org.graylog.security.shares.GranteeService;
import org.graylog.security.shares.PluggableEntityService;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.security.Permission;
import org.graylog2.rest.models.SortOrder;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class RecentActivityService {
    public static final String COLLECTION_NAME = "recent_activity";
    private final EventBus eventBus;
    private final GRNRegistry grnRegistry;
    private final PermissionAndRoleResolver permissionAndRoleResolver;

    private static final long MAXIMUM_RECENT_ACTIVITIES = 10000;
    private final MongoCollection<RecentActivityDTO> db;
    private final MongoPaginationHelper<RecentActivityDTO> pagination;
    private final DBGrantService grantService;
    private final GranteeService granteeService;
    private final PluggableEntityService pluggableEntityService;
    private CapabilityRegistry capabilityRegistry;

    @Inject
    public RecentActivityService(final MongoCollections mongoCollections,
                                 final MongoConnection mongoConnection,
                                 final EventBus eventBus,
                                 final GRNRegistry grnRegistry,
                                 final PermissionAndRoleResolver permissionAndRoleResolver,
                                 final DBGrantService grantService,
                                 final GranteeService granteeService,
                                 final PluggableEntityService pluggableEntityService,
                                 final CapabilityRegistry capabilityRegistry) {
        this(mongoCollections, mongoConnection, eventBus, grnRegistry, permissionAndRoleResolver, MAXIMUM_RECENT_ACTIVITIES,
                grantService, granteeService, pluggableEntityService, capabilityRegistry);
    }

    /*
     * Constructor to set a low maximum in tests to check the capped collection.
     */
    protected RecentActivityService(final MongoCollections mongoCollections,
                                    final MongoConnection mongoConnection,
                                    final EventBus eventBus,
                                    final GRNRegistry grnRegistry,
                                    final PermissionAndRoleResolver permissionAndRoleResolver,
                                    final long maximum,
                                    final DBGrantService grantService,
                                    final GranteeService granteeService,
                                    final PluggableEntityService pluggableEntityService,
                                    final CapabilityRegistry capabilityRegistry) {
        this.grantService = grantService;
        this.granteeService = granteeService;
        this.pluggableEntityService = pluggableEntityService;
        this.capabilityRegistry = capabilityRegistry;
        final var mongodb = mongoConnection.getMongoDatabase();
        if (!mongodb.listCollectionNames().into(new HashSet<>()).contains(COLLECTION_NAME)) {
            mongodb.createCollection(COLLECTION_NAME, new CreateCollectionOptions().capped(true).sizeInBytes(maximum * 1024).maxDocuments(maximum));
        }
        this.db = mongoCollections.collection(COLLECTION_NAME, RecentActivityDTO.class);
        this.grnRegistry = grnRegistry;
        this.permissionAndRoleResolver = permissionAndRoleResolver;
        this.eventBus = eventBus;
        this.pagination = mongoCollections.paginationHelper(this.db);

        db.createIndex(new BasicDBObject(RecentActivityDTO.FIELD_ITEM_GRN, 1));
    }

    private void postRecentActivity(final RecentActivityEvent event) {
        eventBus.post(event);
    }

    public void create(String id, GRNType grn, SearchUser user) {
        create(id, grn, user.getUser());
    }

    public void create(String id, GRNType grn, User user) {
        postRecentActivity(new RecentActivityEvent(ActivityType.CREATE, grnRegistry.newGRN(grn, id), user.getFullName()));
    }

    public void update(String id, GRNType grn, SearchUser user) {
        update(id, grn, user.getUser());
    }

    public void update(String id, GRNType grn, User user) {
        postRecentActivity(new RecentActivityEvent(ActivityType.UPDATE, grnRegistry.newGRN(grn, id), user.getFullName()));
    }

    public void delete(String id, GRNType grn, String title, SearchUser user) {
        delete(id, grn, title, user.getUser());
    }

    public void delete(String id, GRNType grn, String title, User user) {
        postRecentActivity(new RecentActivityEvent(ActivityType.DELETE, grnRegistry.newGRN(grn, id), title, user.getFullName()));
    }

    public PaginatedList<RecentActivityDTO> findRecentActivitiesFor(SearchUser user, int page, int perPage) {
        // show the most recent activities first
        var sort = SortOrder.DESCENDING.toBsonSort(RecentActivityDTO.FIELD_TIMESTAMP);
        // no permission check for local admin
        if (user.getUser().isLocalAdmin()) {
            return pagination
                    .perPage(perPage)
                    .sort(sort)
                    .includeGrandTotal(true)
                    .page(page);
        }

        // filter relevant activities by permissions
        final var principal = grnRegistry.newGRN(GRNTypes.USER, user.getUser().getId());
        final var grantees = permissionAndRoleResolver.resolveGrantees(principal).stream().map(GRN::toString).toList();
        final var sharedEntities = getShareGRNsFor(principal);
        return pagination
                .perPage(perPage)
                .sort(sort)
                .includeGrandTotal(true)
                .page(page, activity -> {
                    final var itemGRN = activity.itemGrn();
                    final var hasAnyPermission = capabilityRegistry.getPermissions(Capability.VIEW, itemGRN.grnType())
                            .stream()
                            .map(Permission::permission)
                            .anyMatch(permission -> user.isPermitted(permission, itemGRN.entity()));
                    return hasAnyPermission || grantees.contains(activity.grantee()) || sharedEntities.contains(itemGRN);
                });
    }

    private Set<GRN> getShareGRNsFor(GRN grantee) {
        // Get all aliases for the grantee to make sure we find all entities the grantee has access to
        final Set<GRN> granteeAliases = granteeService.getGranteeAliases(grantee);

        final ImmutableSet<GrantDTO> grants = grantService.getForGranteesOrGlobalWithCapability(granteeAliases, Capability.VIEW);

        return grants.stream()
                .map(GrantDTO::target)
                .flatMap(pluggableEntityService::expand)
                .filter(pluggableEntityService.excludeTypesFilter())
                .collect(Collectors.toSet());
    }

    public void deleteAllEntriesForEntity(GRN grn) {
        db.deleteMany(Filters.eq(RecentActivityDTO.FIELD_ITEM_GRN, grn.toString()));
    }

    public void save(RecentActivityDTO activity) {
        db.insertOne(activity);
    }
}
