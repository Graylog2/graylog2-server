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
package org.graylog2.system.urlallowlist;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.cluster.ClusterConfigChangedEvent;
import org.graylog2.plugin.cluster.ClusterConfigService;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class UrlAllowlistService extends AbstractIdleService {
    private volatile UrlAllowlist cachedAllowlist = null;

    private final EventBus eventBus;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public UrlAllowlistService(EventBus eventBus, ClusterConfigService clusterConfigService) {
        this.eventBus = eventBus;
        this.clusterConfigService = clusterConfigService;
    }

    /**
     * Gets the allowlist by reading from the cluster configuration.
     *
     * <p>There should always be an allowlist which is created by an initial migration but if there is none, we return a
     * disabled one, which will consider all URLs as allowlisted.</p>
     *
     * <p> This is  because we can't easily guarantee that migrations are run before other services are started. On a
     * system that didn't have an allowlist before, we have to add the URLs configured e.g. in lookup table data adapters
     * to the allowlist by running a migration. If the services start before the migration has run, the configured URLs
     * have to pass the allowlist though, otherwise the services won't be able to run properly. Once the migration has
     * run, these URLs will have been added to allowlist and we are fine.</p>
     */
    public UrlAllowlist getAllowlist() {
        UrlAllowlist allowlist = cachedAllowlist;
        if (allowlist == null) {
            cachedAllowlist = allowlist = clusterConfigService.getOrDefault(UrlAllowlist.class,
                    UrlAllowlist.create(Collections.emptyList(), true));
        }
        return allowlist;
    }

    public void saveAllowlist(UrlAllowlist allowlist) {
        clusterConfigService.write(allowlist);
        cachedAllowlist = null;
    }

    public boolean isAllowlisted(String url) {
        return getAllowlist().isAllowlisted(url);
    }

    public Optional<AllowlistEntry> getEntry(String id) {
        return getAllowlist().entries()
                .stream()
                .filter(entry -> entry.id()
                        .equals(id))
                .findFirst();
    }

    public void addEntry(AllowlistEntry entry) {
        final UrlAllowlist modified = addEntry(getAllowlist(), entry);
        saveAllowlist(modified);
    }

    public void removeEntry(String id) {
        UrlAllowlist modified = removeEntry(getAllowlist(), id);
        saveAllowlist(modified);
    }

    @Subscribe
    public void handleAllowlistUpdated(ClusterConfigChangedEvent event) {
        if (UrlAllowlist.class.getCanonicalName().equals(event.type())) {
            cachedAllowlist = null;
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
    UrlAllowlist addEntry(UrlAllowlist allowlist, AllowlistEntry entry) {
        final LinkedHashMap<String, AllowlistEntry> entriesMap = allowlist.entries()
                .stream()
                .collect(Collectors.toMap(AllowlistEntry::id, Function.identity(),
                        (a, b) -> {throw new IllegalStateException("Duplicate key '" + a + "'.");},
                        LinkedHashMap::new));
        entriesMap.put(entry.id(), entry);
        return allowlist.toBuilder()
                .entries(ImmutableList.copyOf(entriesMap.values()))
                .build();
    }

    @VisibleForTesting
    UrlAllowlist removeEntry(UrlAllowlist allowlist, String id) {
        List<AllowlistEntry> entries = allowlist.entries()
                .stream()
                .filter(entry -> !entry.id()
                        .equals(id))
                .collect(Collectors.toList());
        return allowlist.toBuilder()
                .entries(entries)
                .build();
    }
}
