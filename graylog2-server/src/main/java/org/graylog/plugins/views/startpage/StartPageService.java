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
import org.graylog.plugins.views.favorites.Favorite;
import org.graylog.plugins.views.favorites.FavoriteDTO;
import org.graylog.plugins.views.favorites.FavoritesForUserDTO;
import org.graylog.plugins.views.favorites.FavoritesService;
import org.graylog.plugins.views.search.views.ViewResolver;
import org.graylog.plugins.views.search.views.ViewResolverDecoder;
import org.graylog.plugins.views.startpage.lastOpened.LastOpened;
import org.graylog.plugins.views.startpage.lastOpened.LastOpenedDTO;
import org.graylog.plugins.views.startpage.lastOpened.LastOpenedForUserDTO;
import org.graylog.plugins.views.startpage.lastOpened.LastOpenedService;
import org.graylog.plugins.views.startpage.recentActivities.RecentActivity;
import org.graylog.plugins.views.startpage.recentActivities.RecentActivityDTO;
import org.graylog.plugins.views.startpage.recentActivities.RecentActivityService;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.lookup.Catalog;
import org.graylog2.rest.models.PaginatedResponse;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class StartPageService {
    private final Catalog catalog;
    private final LastOpenedService lastOpenedService;
    private final RecentActivityService recentActivityService;
    private final Map<String, ViewResolver> viewResolvers;
    private final long MAXIMUM_LAST_OPENED_PER_USER = 100;

    @Inject
    public StartPageService(Catalog catalog,
                            LastOpenedService lastOpenedService,
                            RecentActivityService recentActivityService,
                            Map<String, ViewResolver> viewResolvers,
                            EventBus eventBus) {
        this.catalog = catalog;
        this.lastOpenedService = lastOpenedService;
        this.recentActivityService = recentActivityService;
        this.viewResolvers = viewResolvers;
        eventBus.register(this);
    }

    public PaginatedResponse<LastOpened> findLastOpenedFor(final SearchUser searchUser, final int page, final int perPage) {
        var items = lastOpenedService
                .findForUser(searchUser)
                .orElse(new LastOpenedForUserDTO(searchUser.getUser().getId(), List.of()))
                .items()
                .stream()
                .map(this::createLastOpened)
                .toList();

        return PaginatedResponse.create("lastOpened", new PaginatedList<>(PaginatedDbService.getPage(items, page, perPage), items.size(), page, perPage));
    }

    private LastOpened unknownEntity(final LastOpenedDTO dto) {
        return new LastOpened(dto.id(), "Unknown entity: " + dto.id(), "Unknown entity: " + dto.id(), dto.timestamp());
    }

    private String convertViewType(final ViewDTO viewDTO) {
        return viewDTO.type().name().toLowerCase(Locale.ROOT);
    }

    private LastOpened createLastOpened(final LastOpenedDTO dto) {
        final ViewResolverDecoder decoder = new ViewResolverDecoder(dto.id());
        if (decoder.isResolverViewId()) {
            final ViewResolver viewResolver = viewResolvers.get(decoder.getResolverName());
            if (viewResolver != null) {
                return viewResolver
                        .get(decoder.getViewId())
                        .map(view -> new LastOpened(dto.id(), this.convertViewType(view), view.title(), dto.timestamp()))
                        .orElse(this.unknownEntity(dto));
            } else {
                return this.unknownEntity(dto);
            }
        } else {
            return new LastOpened(dto.id(), catalog.getType(dto.id()), catalog.getTitle(dto.id()), dto.timestamp());
        }
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

    /*
     * filters a given Id from the middle of the list if it exists and removes one item if necessary to stay in the limit if we add another item at the top
     */
    protected static List<LastOpenedDTO> filterForExistingIdAndCapAtMaximum(final LastOpenedForUserDTO loi, final String id, final long max) {
        return loi.items().stream().filter(i -> !i.id().equals(id)).limit(max - 1).toList();
    }

    public void addLastOpenedFor(final ViewDTO view, final SearchUser searchUser) {
        final var lastOpenedItems = lastOpenedService.findForUser(searchUser);
        final var item = new LastOpenedDTO(view.id(), DateTime.now(DateTimeZone.UTC));
        if(lastOpenedItems.isPresent()) {
            var loi = lastOpenedItems.get();
            var items = filterForExistingIdAndCapAtMaximum(loi, item.id(), MAXIMUM_LAST_OPENED_PER_USER);
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
