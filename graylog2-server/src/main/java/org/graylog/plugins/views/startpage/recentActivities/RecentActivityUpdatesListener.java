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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog.security.events.EntitySharesUpdateEvent;

import jakarta.inject.Inject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public class RecentActivityUpdatesListener {
    private final RecentActivityService recentActivityService;

    @Inject
    public RecentActivityUpdatesListener(EventBus eventBus, RecentActivityService recentActivityService) {
        this.recentActivityService = recentActivityService;
        eventBus.register(this);
    }

    @Subscribe
    public void createUpdateRecentActivityFor(final RecentActivityEvent event) {
        // first, if we delete an entity, we have to remove old entries from the Recent Activities collection because some info is no longer available from the catalog.
        if (event.activityType().equals(ActivityType.DELETE)) {
            recentActivityService.deleteAllEntriesForEntity(event.grn());
        }

        // save the new activity
        recentActivityService.save(RecentActivityDTO.builder()
                .activityType(event.activityType())
                .itemGrn(event.grn())
                .itemTitle(event.itemTitle())
                .userName(event.userName())
                .build());
    }

    private <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    @Subscribe
    public void createRecentActivityFor(final EntitySharesUpdateEvent event) {
        // TODO: maybe remove the filter again? It should be unnecessary, was just a try to remove creation of duplicates
        event.creates().stream().filter(distinctByKey(EntitySharesUpdateEvent.Share::grantee))
                .forEach(e -> recentActivityService.save(RecentActivityDTO.builder()
                        .activityType(ActivityType.SHARE)
                        .itemGrn(event.entity())
                        .userName(event.user().getFullName())
                        .grantee(e.grantee().toString())
                        .build())
                );

        // TODO: maybe remove the filter again? It should be unnecessary, was just a try to remove creation of duplicates
        event.deletes().stream().filter(distinctByKey(EntitySharesUpdateEvent.Share::grantee))
                .forEach(e -> recentActivityService.save(RecentActivityDTO.builder()
                        .activityType(ActivityType.UNSHARE)
                        .itemGrn(event.entity())
                        .userName(event.user().getFullName())
                        .grantee(e.grantee().toString())
                        .build()));
    }

}
