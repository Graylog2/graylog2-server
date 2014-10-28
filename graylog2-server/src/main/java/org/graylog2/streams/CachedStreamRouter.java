/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.streams;

import com.codahale.metrics.MetricRegistry;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.graylog2.Configuration;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class CachedStreamRouter extends StreamRouter {
    private static final Logger LOG = LoggerFactory.getLogger(CachedStreamRouter.class);
    private static final AtomicReference<LoadingCache<String, List<Stream>>> CACHED_STREAMS = new AtomicReference<>();
    private static final AtomicReference<LoadingCache<Stream, List<StreamRule>>> CACHED_STREAM_RULES = new AtomicReference<>();
    private final LoadingCache<String, List<Stream>> cachedStreams;
    private final LoadingCache<Stream, List<StreamRule>> cachedStreamRules;

    @Inject
    public CachedStreamRouter(StreamService streamService,
                              StreamRuleService streamRuleService,
                              MetricRegistry metricRegistry,
                              Configuration configuration,
                              NotificationService notificationService) {
        super(streamService, streamRuleService, metricRegistry, configuration, notificationService);

        CACHED_STREAMS.compareAndSet(null, buildStreamsLoadingCache());
        CACHED_STREAM_RULES.compareAndSet(null, buildStreamRulesLoadingCache());

        // The getStreams and getStreamRules methods might be called multiple times per message. Avoid contention on the
        // AtomicReference by storing the LoadingCaches in a field.
        cachedStreams = CACHED_STREAMS.get();
        cachedStreamRules = CACHED_STREAM_RULES.get();
    }

    private LoadingCache<String, List<Stream>> buildStreamsLoadingCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(1, TimeUnit.SECONDS)
                .build(
                        new CacheLoader<String, List<Stream>>() {
                            @Override
                            public List<Stream> load(final String s) throws Exception {
                                return CachedStreamRouter.super.getStreams();
                            }
                        }
                );
    }

    private LoadingCache<Stream, List<StreamRule>> buildStreamRulesLoadingCache() {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.SECONDS)
                .build(
                        new CacheLoader<Stream, List<StreamRule>>() {
                            @Override
                            public List<StreamRule> load(final Stream s) throws Exception {
                                return CachedStreamRouter.super.getStreamRules(s);
                            }
                        }
                );
    }

    @Override
    protected List<Stream> getStreams() {
        List<Stream> result = null;
        try {
            result = cachedStreams.get("streams");
        } catch (ExecutionException e) {
            LOG.error("Caught exception while fetching from cache", e);
        }
        return result;
    }

    @Override
    protected List<StreamRule> getStreamRules(Stream stream) {
        List<StreamRule> result = null;
        try {
            result = cachedStreamRules.get(stream);
        } catch (ExecutionException e) {
            LOG.error("Caught exception while fetching from cache", e);
        }

        return result;
    }

}
