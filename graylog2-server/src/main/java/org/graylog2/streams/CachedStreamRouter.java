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

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class CachedStreamRouter extends StreamRouter {
    private static final Logger LOG = LoggerFactory.getLogger(CachedStreamRouter.class);
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
