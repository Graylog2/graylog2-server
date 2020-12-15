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
package org.graylog2.grok;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.UncheckedExecutionException;
import io.krakens.grok.api.Grok;
import io.krakens.grok.api.GrokCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.cache.CacheLoader.asyncReloading;

@Singleton
public class GrokPatternRegistry {
    private static final Logger log = LoggerFactory.getLogger(GrokPatternRegistry.class);

    private final GrokPatternService grokPatternService;

    private final AtomicReference<Set<GrokPattern>> patterns = new AtomicReference<>(Collections.emptySet());
    private final LoadingCache<String, Grok> grokCache;
    private final LoadingCache<String, Grok> grokCacheNamedOnly;

    @Inject
    public GrokPatternRegistry(EventBus serverEventBus,
                               GrokPatternService grokPatternService,
                               @Named("daemonScheduler") ScheduledExecutorService daemonExecutor) {
        this.grokPatternService = grokPatternService;

        grokCache = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.MINUTES) // prevent from hanging on to memory forever
                .build(asyncReloading(new GrokReloader(false), daemonExecutor));

        grokCacheNamedOnly = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.MINUTES) // prevent from hanging on to memory forever
                .build(asyncReloading(new GrokReloader(true), daemonExecutor));

        // trigger initial loading
        reload();

        serverEventBus.register(this);
    }

    @Subscribe
    public void grokPatternsUpdated(GrokPatternsUpdatedEvent event) {
        // for now we simply reload everything and don't care what exactly has changed
        reload();
    }

    @Subscribe
    public void grokPatternsDeleted(GrokPatternsDeletedEvent event) {
        // for now we simply reload everything and don't care what exactly has changed
        reload();
    }

    public boolean grokPatternExists(String patternName) {
        return patterns.get().stream().anyMatch(pattern -> pattern.name().contains(patternName));
    }

    public Grok cachedGrokForPattern(String pattern) {
        return cachedGrokForPattern(pattern, false);
    }

    public Grok cachedGrokForPattern(String pattern, boolean namedCapturesOnly) {
        try {
            if (namedCapturesOnly) {
                return grokCacheNamedOnly.get(pattern);
            } else {
                return grokCache.get(pattern);
            }
        } catch (UncheckedExecutionException | ExecutionException e) {
            final Throwable rootCause = Throwables.getRootCause(e);
            log.error("Unable to load grok pattern {} into cache", pattern, rootCause);
            throw new RuntimeException(rootCause);
        }
    }

    private void reload() {
        final Set<GrokPattern> grokPatterns = grokPatternService.loadAll();
        patterns.set(grokPatterns);
        grokCache.invalidateAll();
        grokCacheNamedOnly.invalidateAll();
    }

    public Set<GrokPattern> patterns() {
        return patterns.get();
    }

    private class GrokReloader extends CacheLoader<String, Grok> {
        private final boolean namedCapturesOnly;

        GrokReloader(boolean namedCapturesOnly) {
            this.namedCapturesOnly = namedCapturesOnly;
        }

        @Override
        public Grok load(@Nonnull String pattern) throws Exception {
            final GrokCompiler grokCompiler = GrokCompiler.newInstance();
            for (GrokPattern grokPattern : patterns()) {
                grokCompiler.register(grokPattern.name(), grokPattern.pattern());
            }
            return grokCompiler.compile(pattern, namedCapturesOnly);
        }
    }
}
