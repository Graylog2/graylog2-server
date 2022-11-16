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

import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog2.contentpacks.ContentPackService;
import org.graylog2.database.NotFoundException;

import javax.inject.Inject;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

public class DynamicStartPageService {
    private final ContentPackService contentPackService;
    private final LastOpenedService lastOpenedService;
    private final RecentActivityService recentActivityService;
    private final PinnedItemsService pinnedItemsService;

    @Inject
    public DynamicStartPageService(ContentPackService contentPackService, LastOpenedService lastOpenedService, RecentActivityService recentActivityService, PinnedItemsService pinnedItemsService) {
        this.contentPackService = contentPackService;
        this.lastOpenedService = lastOpenedService;
        this.recentActivityService = recentActivityService;
        this.pinnedItemsService = pinnedItemsService;
    }

    public List<LastOpenedItem> findLastOpenedFor(final SearchUser searchUser) throws NotFoundException {
        var catalog = contentPackService.getEntityExcerpts();
        return lastOpenedService
                .findForUser(searchUser)
                .orElseThrow(NotFoundException::new)
                .items()
                .stream()
                .map(i -> new LastOpenedItem(i, catalog.get(i).type().name(), catalog.get(i).title()))
                .collect(Collectors.toList());
    }

    public List<PinnedItem> findPinnedItemsFor(final SearchUser searchUser) throws NotFoundException {
        var catalog = contentPackService.getEntityExcerpts();
        return pinnedItemsService
                .findForUser(searchUser)
                .orElseThrow(NotFoundException::new)
                .items()
                .stream()
                .map(i -> new PinnedItem(i, catalog.get(i).type().name(), catalog.get(i).title()))
                .collect(Collectors.toList());
    }

    public List<RecentActivity> findRecentActivityFor(final SearchUser searchUser) {
        var catalog = contentPackService.getEntityExcerpts();
        return recentActivityService
                .streamAll()
                .filter(searchUser::canSeeActivity)
                .limit(10)
                .map(i -> new RecentActivity(i.id(), i.activityType(), catalog.get(i.itemId()).type().name(), i.itemId(), catalog.get(i.itemId()).title(), i.timestamp()))
                .collect(Collectors.toList());
    }

    public void addLastOpenedFor(final ViewDTO view, final SearchUser searchUser) {
        var lastOpenedItems = lastOpenedService.findForUser(searchUser);
        if(lastOpenedItems.isPresent()) {
            var id = view.id();
            var items = new LinkedHashSet<>(lastOpenedItems.get().items());
            if(items.contains(id)) {
                items.remove(id);
            }
            items.add(id);
            lastOpenedItems.get().items().clear();
            lastOpenedItems.get().items().addAll(items);
            lastOpenedService.save(lastOpenedItems.get());
        } else {
            var items = LastOpenedItemsDTO.builder().userId(searchUser.getUser().getId()).items(Collections.singletonList(view.id())).build();
            lastOpenedService.create(items, searchUser);
        }
    }

    public void addPinnedItemFor(final String id, final SearchUser searchUser) {
        var pinnedItems = pinnedItemsService.findForUser(searchUser);
        if(pinnedItems.isPresent()) {
            var dto = pinnedItems.get();
            if(!dto.items().contains(id)) {
                dto.items().add(id);
            }
            pinnedItemsService.save(dto);
        } else {
            var items = PinnedItemsDTO.builder().userId(searchUser.getUser().getId()).items(Collections.singletonList(id)).build();
            pinnedItemsService.create(items, searchUser);
        }
    }
}
