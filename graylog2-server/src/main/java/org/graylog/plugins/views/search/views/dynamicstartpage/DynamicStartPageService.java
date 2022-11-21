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
package org.graylog.plugins.views.search.views.dynamicstartpage;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.security.events.EntitySharesUpdateEvent;
import org.graylog2.contentpacks.ContentPackService;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.PaginatedResponse;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DynamicStartPageService {
    private final ContentPackService contentPackService;
    private final LastOpenedService lastOpenedService;
    private final RecentActivityService recentActivityService;
    private final PinnedItemsService pinnedItemsService;
    private final EventBus eventBus;

    private final long MAXIMUM_ITEMS = 10;
    private final long MAXIMUM_ACTIVITY = 100;

    @Inject
    public DynamicStartPageService(ContentPackService contentPackService,
                                   LastOpenedService lastOpenedService,
                                   RecentActivityService recentActivityService,
                                   PinnedItemsService pinnedItemsService,
                                   EventBus eventBus) {
        this.contentPackService = contentPackService;
        this.lastOpenedService = lastOpenedService;
        this.recentActivityService = recentActivityService;
        this.pinnedItemsService = pinnedItemsService;
        this.eventBus = eventBus;
        this.eventBus.register(this);
    }

    private <T> List<T> getPage(List<T> sourceList, int page, int pageSize) {
        if(pageSize <= 0 || page <= 0) {
            throw new IllegalArgumentException("invalid page size: " + pageSize);
        }

        int fromIndex = (page - 1) * pageSize;
        if(sourceList == null || sourceList.size() <= fromIndex){
            return Collections.emptyList();
        }

        // toIndex exclusive
        return sourceList.subList(fromIndex, Math.min(fromIndex + pageSize, sourceList.size()));
    }

    public PaginatedResponse<LastOpenedItem> findLastOpenedFor(final SearchUser searchUser, int page, int perPage) throws NotFoundException {
        var catalog = contentPackService.getEntityExcerpts();
        var items = lastOpenedService
                .findForUser(searchUser)
                .orElse(LastOpenedItemsDTO.builder().userId(searchUser.getUser().getId()).build())
                .items()
                .stream()
                .map(i -> new LastOpenedItem(i, catalog.get(i).type().name(), catalog.get(i).title()))
                .collect(Collectors.toList());
        Collections.reverse(items);

        return PaginatedResponse.create("lastOpened", new PaginatedList<>(getPage(items, page, perPage), items.size(), page, perPage));
    }

    public PaginatedResponse<PinnedItem> findPinnedItemsFor(final SearchUser searchUser, int page, int perPage) throws NotFoundException {
        var catalog = contentPackService.getEntityExcerpts();
        var items = pinnedItemsService
                .findForUser(searchUser)
                .orElse(PinnedItemsDTO.builder().userId(searchUser.getUser().getId()).build())
                .items()
                .stream()
                .map(i -> new PinnedItem(i, catalog.get(i).type().name(), catalog.get(i).title()))
                .collect(Collectors.toList());
        Collections.reverse(items);
        return PaginatedResponse.create("pinnedItems", new PaginatedList<>(getPage(items, page, perPage), items.size(), page, perPage));
    }

    public PaginatedResponse<RecentActivity> findRecentActivityFor(final SearchUser searchUser, int page, int perPage) {
        var catalog = contentPackService.getEntityExcerpts();
        var items = recentActivityService
                .streamAllInReverseOrder()
                .filter(searchUser::canSeeActivity)
                .limit(MAXIMUM_ACTIVITY)
                .map(i -> new RecentActivity(i.id(), i.activityType(), catalog.get(i.itemId()).type().name(), i.itemId(), catalog.get(i.itemId()).title(), i.timestamp()))
                .collect(Collectors.toList());
        return PaginatedResponse.create("recentActivity", new PaginatedList<>(getPage(items, page, perPage), items.size(), page, perPage));
    }

    public void addLastOpenedFor(final ViewDTO view, final SearchUser searchUser) {
        var lastOpenedItems = lastOpenedService.findForUser(searchUser);
        if(lastOpenedItems.isPresent()) {
            final var id = view.id();
            final var items = lastOpenedItems.get().items();
            if(items.contains(id)) {
                items.remove(id);
            }
            if(items.size() >= MAXIMUM_ITEMS) {
                items.remove(0);
            }
            items.add(id);
            lastOpenedService.save(lastOpenedItems.get());
        } else {
            var items = LastOpenedItemsDTO.builder().userId(searchUser.getUser().getId()).items(Collections.singletonList(view.id())).build();
            lastOpenedService.create(items, searchUser);
        }
    }

    public void addPinnedItemFor(final String id, final SearchUser searchUser) {
        var pinnedItems = pinnedItemsService.findForUser(searchUser);
        if(pinnedItems.isPresent()) {
            final var items = pinnedItems.get().items();
            if(items.contains(id)) {
                items.remove(id);
            }
            if(items.size() >= MAXIMUM_ITEMS) {
                items.remove(0);
            }
            items.add(id);
            pinnedItemsService.save(pinnedItems.get());
        } else {
            var items = PinnedItemsDTO.builder().userId(searchUser.getUser().getId()).items(Collections.singletonList(id)).build();
            pinnedItemsService.create(items, searchUser);
        }
    }

    @Subscribe
    public void createRecentActivityFor(EntitySharesUpdateEvent updateEvent) {
 //       updateEvent.creates()
        recentActivityService.save(RecentActivityDTO.builder().activityType(ActivityType.UPDATE).itemId(updateEvent.entity().entity()).build());
    }
}
