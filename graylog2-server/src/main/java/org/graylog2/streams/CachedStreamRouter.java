/*
 * Copyright 2012-2014 TORCH GmbH
 *
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

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class CachedStreamRouter extends StreamRouter {
    private static LoadingCache<String, List<Stream>> cachedStreams;
    private static LoadingCache<Stream, List<StreamRule>> cachedStreamRules;

    @Inject
    public CachedStreamRouter(StreamService streamService,
                              StreamRuleService streamRuleService,
                              MetricRegistry metricRegistry,
                              Configuration configuration,
                              NotificationService notificationService) {
        super(streamService, streamRuleService, metricRegistry, configuration, notificationService);
    }

    @Override
    protected List<Stream> getStreams() {
        if (cachedStreams == null)
            cachedStreams = CacheBuilder.newBuilder()
                    .maximumSize(1)
                    .expireAfterWrite(1, TimeUnit.SECONDS)
                    .build(
                            new CacheLoader<String, List<Stream>>() {
                                @Override
                                public List<Stream> load(String s) throws Exception {
                                    return superGetStreams();
                                }
                            }
                    );
        List<Stream> result = null;
        try {
            result = cachedStreams.get("streams");
        } catch (ExecutionException e) {
            LOG.error("Caught exception while fetching from cache", e);
        }
        return result;
    }

    private List<Stream> superGetStreams() {
        return super.getStreams();
    }

    @Override
    protected List<StreamRule> getStreamRules(Stream stream) {
        if (cachedStreamRules == null)
            cachedStreamRules = CacheBuilder.newBuilder()
                    .expireAfterWrite(1, TimeUnit.SECONDS)
                    .build(
                            new CacheLoader<Stream, List<StreamRule>>() {
                                @Override
                                public List<StreamRule> load(Stream s) throws Exception {
                                    return superGetStreamRules(s);
                                }
                            }
                    );
        List<StreamRule> result = null;
        try {
            result = cachedStreamRules.get(stream);
        } catch (ExecutionException e) {
            LOG.error("Caught exception while fetching from cache", e);
        }

        return result;
    }

    private List<StreamRule> superGetStreamRules(Stream stream) {
        return super.getStreamRules(stream);
    }
}
