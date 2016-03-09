/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.grok;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import oi.thekraken.grok.api.Grok;
import org.graylog2.events.ClusterEventBus;
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

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.cache.CacheLoader.asyncReloading;

@Singleton
public class GrokPatternRegistry {
    private static final Logger log = LoggerFactory.getLogger(GrokPatternRegistry.class);

    private final GrokPatternService grokPatternService;
    private final ScheduledExecutorService daemonExecutor;

    private final AtomicReference<Set<GrokPattern>> patterns = new AtomicReference<>(Collections.emptySet());
    private final LoadingCache<String, Grok> grokCache;

    @Inject
    public GrokPatternRegistry(@ClusterEventBus EventBus clusterBus,
                               GrokPatternService grokPatternService,
                               @Named("daemonScheduler") ScheduledExecutorService daemonExecutor) {
        this.grokPatternService = grokPatternService;
        this.daemonExecutor = daemonExecutor;

        grokCache = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.MINUTES) // prevent from hanging on to memory forever
                .build(asyncReloading(new GrokReloader(), daemonExecutor));

        // trigger initial loading
        reload();

        clusterBus.register(this);
    }

    @Subscribe
    public void grokPatternsChanged(GrokPatternsChangedEvent event) {
        // for now we simply reload everything and don't care what exactly has changed
        daemonExecutor.execute(this::reload);
    }

    public Grok cachedGrokForPattern(String pattern) {
        try {
            return grokCache.get(pattern);
        } catch (ExecutionException e) {
            log.error("Unable to load grok pattern " + pattern + " into cache", e);
            throw new RuntimeException(e);
        }
    }

    private void reload() {
        final Set<GrokPattern> grokPatterns = grokPatternService.loadAll();
        patterns.set(grokPatterns);
        grokCache.invalidateAll();
    }

    public Set<GrokPattern> patterns() {
        return patterns.get();
    }

    private class GrokReloader extends CacheLoader<String, Grok> {
        @Override
        public Grok load(@Nonnull String pattern) throws Exception {
            final Grok grok = new Grok();
            for (GrokPattern grokPattern : patterns()) {
                if (!isNullOrEmpty(grokPattern.name) || isNullOrEmpty(grokPattern.pattern)) {
                    grok.addPattern(grokPattern.name, grokPattern.pattern);
                }
            }
            grok.compile(pattern);
            return grok;
        }
    }
}
