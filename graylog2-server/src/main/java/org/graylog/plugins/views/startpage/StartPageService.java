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
package org.graylog.plugins.views.startpage;

import com.google.common.eventbus.EventBus;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.startpage.lastOpened.LastOpened;
import org.graylog.plugins.views.startpage.lastOpened.LastOpenedDTO;
import org.graylog.plugins.views.startpage.lastOpened.LastOpenedForUserDTO;
import org.graylog.plugins.views.startpage.lastOpened.LastOpenedService;
import org.graylog.plugins.views.startpage.recentActivities.RecentActivity;
import org.graylog.plugins.views.startpage.recentActivities.RecentActivityService;
import org.graylog.plugins.views.startpage.title.StartPageItemTitleRetriever;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.PaginatedResponse;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

public class StartPageService {
    private final GRNRegistry grnRegistry;
    private final LastOpenedService lastOpenedService;
    private final RecentActivityService recentActivityService;
    private final StartPageItemTitleRetriever startPageItemTitleRetriever;
    private final long MAXIMUM_LAST_OPENED_PER_USER = 100;

    @Inject
    public StartPageService(GRNRegistry grnRegistry,
                            LastOpenedService lastOpenedService,
                            RecentActivityService recentActivityService,
                            EventBus eventBus,
                            StartPageItemTitleRetriever startPageItemTitleRetriever) {
        this.grnRegistry = grnRegistry;
        this.lastOpenedService = lastOpenedService;
        this.recentActivityService = recentActivityService;
        this.startPageItemTitleRetriever = startPageItemTitleRetriever;
        eventBus.register(this);
    }

    public PaginatedResponse<LastOpened> findLastOpenedFor(final SearchUser searchUser, final int page, final int perPage) {
        if (perPage <= 0 || page <= 0) {
            throw new IllegalArgumentException("invalid page size: " + perPage);
        }
        var items = lastOpenedService
                .findForUser(searchUser)
                .orElse(new LastOpenedForUserDTO(searchUser.getUser().getId(), List.of()))
                .items()
                .stream()
                .skip((long) (page - 1) * perPage)
                .map(i -> startPageItemTitleRetriever
                        .retrieveTitle(i.grn(), searchUser)
                        .map(title -> new LastOpened(i.grn(), title, i.timestamp()))
                )
                .flatMap(Optional::stream)
                .limit(perPage)
                .toList();

        return PaginatedResponse.create("lastOpened", new PaginatedList<>(items, items.size(), page, perPage));
    }

    public PaginatedResponse<RecentActivity> findRecentActivityFor(final SearchUser searchUser, int page, int perPage) {
        final var items = recentActivityService.findRecentActivitiesFor(searchUser, page, perPage);
        final var mapped = items.stream()
                .map(i -> startPageItemTitleRetriever
                        .retrieveTitle(i.itemGrn(), searchUser)
                        .map(title -> new RecentActivity(i.id(),
                                i.activityType(),
                                i.itemGrn(),
                                title,
                                i.userName(),
                                i.timestamp()))

                )
                .flatMap(Optional::stream)
                .toList();
        return PaginatedResponse.create("recentActivity", new PaginatedList<>(mapped, items.pagination().total(), page, perPage));
    }

    /*
     * filters a given Id from the middle of the list if it exists and removes one item if necessary to stay in the limit if we add another item at the top
     */
    protected static List<LastOpenedDTO> filterForExistingIdAndCapAtMaximum(final LastOpenedForUserDTO loi, final GRN grn, final long max) {
        return loi.items().stream().filter(i -> !i.grn().equals(grn)).limit(max - 1).toList();
    }

    public void addLastOpenedFor(final ViewDTO view, final SearchUser searchUser) {
        final var type = view.type().equals(ViewDTO.Type.DASHBOARD) ? GRNTypes.DASHBOARD : GRNTypes.SEARCH;
        final var lastOpenedItems = lastOpenedService.findForUser(searchUser);
        final var item = new LastOpenedDTO(grnRegistry.newGRN(type, view.id()), DateTime.now(DateTimeZone.UTC));
        if (lastOpenedItems.isPresent()) {
            var loi = lastOpenedItems.get();
            var items = filterForExistingIdAndCapAtMaximum(loi, item.grn(), MAXIMUM_LAST_OPENED_PER_USER);
            loi.items().clear();
            loi.items().add(item);
            loi.items().addAll(items);
            lastOpenedService.save(loi);
        } else {
            var items = new LastOpenedForUserDTO(searchUser.getUser().getId(), List.of(item));
            lastOpenedService.create(items, searchUser);
        }
    }
}
