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
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.startpage.favorites.Favorite;
import org.graylog.plugins.views.startpage.favorites.FavoriteDTO;
import org.graylog.plugins.views.startpage.favorites.FavoritesForUserDTO;
import org.graylog.plugins.views.startpage.favorites.FavoritesService;
import org.graylog.plugins.views.startpage.lastOpened.LastOpenedDTO;
import org.graylog.plugins.views.startpage.lastOpened.LastOpened;
import org.graylog.plugins.views.startpage.lastOpened.LastOpenedForUserDTO;
import org.graylog.plugins.views.startpage.lastOpened.LastOpenedService;
import org.graylog.plugins.views.startpage.recentActivities.RecentActivity;
import org.graylog.plugins.views.startpage.recentActivities.RecentActivityDTO;
import org.graylog.plugins.views.startpage.recentActivities.RecentActivityService;
import org.graylog2.database.PaginatedList;
import org.graylog2.lookup.Catalog;
import org.graylog2.rest.models.PaginatedResponse;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StartPageService {
    private final Catalog catalog;
    private final LastOpenedService lastOpenedService;
    private final RecentActivityService recentActivityService;
    private final FavoritesService favoritesService;

    private final long MAXIMUM_LAST_OPENED_PER_USER = 100;

    @Inject
    public StartPageService(Catalog catalog,
                            LastOpenedService lastOpenedService,
                            RecentActivityService recentActivityService,
                            FavoritesService favoritesService,
                            EventBus eventBus) {
        this.catalog = catalog;
        this.lastOpenedService = lastOpenedService;
        this.recentActivityService = recentActivityService;
        this.favoritesService = favoritesService;
        eventBus.register(this);
    }

    protected <T> List<T> getPage(List<T> sourceList, int page, int pageSize) {
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

    public PaginatedResponse<LastOpened> findLastOpenedFor(final SearchUser searchUser, final int page, final int perPage) {
        var items = lastOpenedService
                .findForUser(searchUser)
                .orElse(new LastOpenedForUserDTO(searchUser.getUser().getId(), List.of()))
                .items()
                .stream()
                .map(i -> new LastOpened(i.id(), catalog.getType(i.id()), catalog.getTitle(i.id()), i.timestamp()))
                .collect(Collectors.toList());
        Collections.reverse(items);

        return PaginatedResponse.create("lastOpened", new PaginatedList<>(getPage(items, page, perPage), items.size(), page, perPage));
    }

    public PaginatedResponse<Favorite> findFavoritesFor(final SearchUser searchUser, final Optional<String> type, final int page, final int perPage) {
        var items = favoritesService
                .findForUser(searchUser)
                .orElse(new FavoritesForUserDTO(searchUser.getUser().getId(), List.of()))
                .items()
                .stream().filter(i -> type.isPresent() ? i.type().equals(type.get()) : true)
                .map(i -> new Favorite(i.id(), i.type(), catalog.getTitle(i.id())))
                .toList();

        return PaginatedResponse.create("favorites", new PaginatedList<>(getPage(items, page, perPage), items.size(), page, perPage));
    }

    private String getType(RecentActivityDTO i) {
        return i.itemType() == null ? catalog.getType(i.itemId()) : i.itemType();
    }

    private String getTitle(RecentActivityDTO i) {
        return i.itemTitle() == null ? catalog.getTitle(i.itemId()) : i.itemTitle();
    }

    public PaginatedResponse<RecentActivity> findRecentActivityFor(final SearchUser searchUser, int page, int perPage) {
        final var items = recentActivityService.findRecentActivitiesFor(searchUser, page, perPage);
        final var mapped = items.stream()
                 .map(i -> new RecentActivity(i.id(),
                        i.activityType(),
                        getType(i),
                        i.itemId(),
                        getTitle(i),
                        i.userName(),
                        i.timestamp())).toList();
        return PaginatedResponse.create("recentActivity", new PaginatedList<>(mapped, items.pagination().total(), page, perPage));
    }

    public void addLastOpenedFor(final ViewDTO view, final SearchUser searchUser) {
        final var lastOpenedItems = lastOpenedService.findForUser(searchUser);
        final var item = new LastOpenedDTO(view.id(), DateTime.now(DateTimeZone.UTC));
        if(lastOpenedItems.isPresent()) {
            var loi = lastOpenedItems.get();
            var items = loi.items().stream().filter(i -> !i.id().equals(item.id())).limit(MAXIMUM_LAST_OPENED_PER_USER - 1).toList();
            loi.items().clear();
            loi.items().addAll(items);
            loi.items().add(item);
            lastOpenedService.save(loi);
        } else {
            var items = new LastOpenedForUserDTO(searchUser.getUser().getId(), List.of(item));
            lastOpenedService.create(items, searchUser);
        }
    }

    public void addFavoriteItemFor(final String id, final SearchUser searchUser) {
        final var favorites = favoritesService.findForUser(searchUser);
        final var item = new FavoriteDTO(id, catalog.getType(id));
        if(favorites.isPresent()) {
            var fi = favorites.get();
            fi.items().add(item);
            favoritesService.save(fi);
        } else {
            var items = new FavoritesForUserDTO(searchUser.getUser().getId(), List.of(item));
            favoritesService.create(items, searchUser);
        }
    }

    public void removeFavoriteItemFor(final String id, final SearchUser searchUser) {
        var favorites = favoritesService.findForUser(searchUser);
        if(favorites.isPresent() && favorites.get().items() != null) {
            var fi = favorites.get();
            var items = fi.items().stream().filter(i -> !i.id().equals(id)).toList();
            fi.items().clear();
            fi.items().addAll(items);
            favoritesService.save(fi);
        }
    }
}
