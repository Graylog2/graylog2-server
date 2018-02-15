package org.graylog.plugins.pipelineprocessor.functions.messages;

import com.google.common.eventbus.EventBus;

import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.junit.Test;

import java.util.Collection;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class StreamCacheServiceTest {
    @Test
    public void getByName() throws Exception {
        final StreamCacheService streamCacheService = new StreamCacheService(new EventBus(), mock(StreamService.class), Executors.newSingleThreadScheduledExecutor());

        // make sure getByName always returns a collection
        final Collection<Stream> streams = streamCacheService.getByName("nonexisting");
        assertThat(streams).isNotNull().isEmpty();
    }

}