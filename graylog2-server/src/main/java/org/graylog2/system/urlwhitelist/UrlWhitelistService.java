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
package org.graylog2.system.urlwhitelist;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.graylog2.cluster.ClusterConfigChangedEvent;
import org.graylog2.plugin.cluster.ClusterConfigService;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class UrlWhitelistService extends AbstractIdleService {
    private volatile UrlWhitelist cachedWhitelist = null;

    private final EventBus eventBus;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public UrlWhitelistService(EventBus eventBus, ClusterConfigService clusterConfigService) {
        this.eventBus = eventBus;
        this.clusterConfigService = clusterConfigService;
    }

    /**
     * Gets the whitelist by reading from the cluster configuration.
     *
     * <p>There should always be a whitelist which is created by an initial migration but if there is none, we return a
     * disabled one, which will consider all URLs as whitelisted.</p>
     *
     * <p> This is  because we can't easily guarantee that migrations are run before other services are started. On a
     * system that didn't have a whitelist before, we have to add the URLs configured e.g. in lookup table data adapters
     * to the whitelist by running a migration. If the services start before the migration has run, the configured URLs
     * have to pass the whitelist though, otherwise the services won't be able to run properly. Once the migration has
     * run, these URLs will have been added to whitelist and we are fine.</p>
     */
    public UrlWhitelist getWhitelist() {
        UrlWhitelist whitelist = cachedWhitelist;
        if (whitelist == null) {
            cachedWhitelist = whitelist = clusterConfigService.getOrDefault(UrlWhitelist.class,
                    UrlWhitelist.create(Collections.emptyList(), true));
        }
        return whitelist;
    }

    public void saveWhitelist(UrlWhitelist whitelist) {
        clusterConfigService.write(whitelist);
        cachedWhitelist = null;
    }

    public boolean isWhitelisted(String url) {
        return getWhitelist().isWhitelisted(url);
    }

    public Optional<WhitelistEntry> getEntry(String id) {
        return getWhitelist().entries()
                .stream()
                .filter(entry -> entry.id()
                        .equals(id))
                .findFirst();
    }

    public void addEntry(WhitelistEntry entry) {
        final UrlWhitelist modified = addEntry(getWhitelist(), entry);
        saveWhitelist(modified);
    }

    public void removeEntry(String id) {
        UrlWhitelist modified = removeEntry(getWhitelist(), id);
        saveWhitelist(modified);
    }

    @Subscribe
    public void handleWhitelistUpdated(ClusterConfigChangedEvent event) {
        if (UrlWhitelist.class.getCanonicalName().equals(event.type())) {
            cachedWhitelist = null;
        }
    }

    @Override
    protected void startUp() {
        eventBus.register(this);
    }

    @Override
    protected void shutDown() {
        eventBus.unregister(this);
    }

    @VisibleForTesting
    UrlWhitelist addEntry(UrlWhitelist whitelist, WhitelistEntry entry) {
        final LinkedHashMap<String, WhitelistEntry> entriesMap = whitelist.entries()
                .stream()
                .collect(Collectors.toMap(WhitelistEntry::id, Function.identity(),
                        (a, b) -> { throw new IllegalStateException("Duplicate key '" + a + "'."); },
                        LinkedHashMap::new));
        entriesMap.put(entry.id(), entry);
        return whitelist.toBuilder()
                .entries(ImmutableList.copyOf(entriesMap.values()))
                .build();
    }

    @VisibleForTesting
    UrlWhitelist removeEntry(UrlWhitelist whitelist, String id) {
        List<WhitelistEntry> entries = whitelist.entries()
                .stream()
                .filter(entry -> !entry.id()
                        .equals(id))
                .collect(Collectors.toList());
        return whitelist.toBuilder()
                .entries(entries)
                .build();
    }
}
