package org.graylog2.outputs;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.OutputService;

import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class CachedOutputRouter extends OutputRouter {
    private static LoadingCache<Stream, Set<MessageOutput>> cachedStreamOutputRoutes;

    @Inject
    public CachedOutputRouter(@DefaultMessageOutput MessageOutput defaultMessageOutput, OutputService outputService, MessageOutputFactory messageOutputFactory) {
        super(defaultMessageOutput, outputService, messageOutputFactory);
    }

    @Override
    protected Set<MessageOutput> getMessageOutputsForStream(Stream stream) {
        if (cachedStreamOutputRoutes == null) {
            cachedStreamOutputRoutes = CacheBuilder.newBuilder()
                    .maximumSize(100)
                    .expireAfterWrite(1, TimeUnit.SECONDS)
                    .build(
                            new CacheLoader<Stream, Set<MessageOutput>>() {
                                @Override
                                public Set<MessageOutput> load(final Stream key) throws Exception {
                                    return superGetMessageOutputsForStream(key);
                                }
                            }
                    );
        }

        try {
            return cachedStreamOutputRoutes.get(stream);
        } catch (ExecutionException e) {
            LOG.error("Caught exception while fetching from output route cache: ", e);
            return null;
        }
    }

    private Set<MessageOutput> superGetMessageOutputsForStream(Stream stream) {
        return super.getMessageOutputsForStream(stream);
    }
}
