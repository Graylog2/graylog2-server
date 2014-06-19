package org.graylog2.bindings.providers;

import org.graylog2.streams.StreamRouter;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class StreamRouterProvider implements Provider<StreamRouter> {
    private static StreamRouter streamRouter = null;

    @Inject
    public StreamRouterProvider(StreamRouter.Factory streamRouterFactory) {
        if (streamRouter == null)
            streamRouter = streamRouterFactory.create(true);
    }

    public StreamRouter get() {
        return streamRouter;
    }
}
