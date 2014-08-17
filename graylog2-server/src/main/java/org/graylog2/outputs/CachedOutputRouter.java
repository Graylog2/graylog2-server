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
package org.graylog2.outputs;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class CachedOutputRouter extends OutputRouter {
    private static final Logger LOG = LoggerFactory.getLogger(CachedOutputRouter.class);
    private static final AtomicReference<LoadingCache<Stream, Set<MessageOutput>>> CACHED_STREAM_OUTPUT_ROUTES = new AtomicReference<>();
    private final LoadingCache<Stream, Set<MessageOutput>> cachedStreamOutputRoutes;

    @Inject
    public CachedOutputRouter(@DefaultMessageOutput MessageOutput defaultMessageOutput,
                              OutputRegistry outputRegistry) {
        super(defaultMessageOutput, outputRegistry);

        CACHED_STREAM_OUTPUT_ROUTES.compareAndSet(null, buildLoadingCache());

        // The getMessageOutputsForStream method might be called multiple times per message. Avoid contention on the
        // AtomicReference by storing the LoadingCache in a field.
        cachedStreamOutputRoutes = CACHED_STREAM_OUTPUT_ROUTES.get();
    }

    private LoadingCache<Stream, Set<MessageOutput>> buildLoadingCache() {
        return CacheBuilder.newBuilder()
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

    @Override
    protected Set<MessageOutput> getMessageOutputsForStream(Stream stream) {
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
