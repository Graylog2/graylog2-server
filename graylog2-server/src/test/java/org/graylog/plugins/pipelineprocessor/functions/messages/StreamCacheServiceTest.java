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
package org.graylog.plugins.pipelineprocessor.functions.messages;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import org.bson.types.ObjectId;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamService;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.streams.StreamImpl.FIELD_INDEX_SET_ID;
import static org.graylog2.streams.StreamImpl.FIELD_TITLE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StreamCacheServiceTest {

    private StreamCacheService cacheService;

    private StreamService streamService;

    @Before
    @SuppressForbidden("Allow using default thread factory")
    public void setUp() throws Exception {
        streamService = mock(StreamService.class);
        cacheService = new StreamCacheService(new EventBus(), streamService,
                                              Executors.newSingleThreadScheduledExecutor());
    }

    @Test
    public void getByName() throws Exception {
        // make sure getByName always returns a collection
        final Collection<Stream> streams = cacheService.getByName("nonexisting");
        assertThat(streams).isNotNull().isEmpty();
    }

    @Test
    public void multipleStreamsBySameName() throws Exception {
        Stream stream1 = createStream(new ObjectId(), ImmutableMap.of(FIELD_TITLE, "title"));
        Stream stream2 = createStream(new ObjectId(), ImmutableMap.of(FIELD_TITLE, "title"));
        Stream stream3 = createStream(new ObjectId(), ImmutableMap.of(FIELD_TITLE, "different title"));

        when(streamService.load(stream1.getId())).thenReturn(stream1);
        when(streamService.load(stream2.getId())).thenReturn(stream2);
        when(streamService.load(stream3.getId())).thenReturn(stream3);
        cacheService.updateStreams(Collections.singleton(stream1.getId()));
        cacheService.updateStreams(Collections.singleton(stream2.getId()));
        cacheService.updateStreams(Collections.singleton(stream3.getId()));

        assertEquals(ImmutableSet.of(stream1, stream2), cacheService.getByName("title"));
        assertEquals(ImmutableSet.of(stream3), cacheService.getByName("different title"));
    }

    @Test
    public void updatesStreamByName() throws Exception {
        ObjectId streamId = new ObjectId();
        Stream stream = createStream(streamId, ImmutableMap.of(FIELD_TITLE, "title"));

        Stream modifiedStream = createStream(streamId, ImmutableMap.of(
                FIELD_TITLE, "title", FIELD_INDEX_SET_ID, "index-set-id"));

        when(streamService.load(stream.getId())).thenReturn(stream);
        cacheService.updateStreams(Collections.singleton(stream.getId()));

        Collection<Stream> streams = cacheService.getByName("title");

        // using assertEquals instead of assertThat(streams).containsExactlyInAnyOrder() here to avoid using the
        // TreeSet comparator for containment checks
        assertEquals(Collections.singleton(stream), streams);

        when(streamService.load(stream.getId())).thenReturn(modifiedStream);
        cacheService.updateStreams(Collections.singleton(modifiedStream.getId()));
        streams = cacheService.getByName("title");

        assertEquals(Collections.singleton(modifiedStream), streams);
    }

    private Stream createStream(ObjectId id, Map<String, Object> fields) {
        return new StreamImpl(id, fields, Collections.emptyList(), Collections.emptySet(), null);
    }
}
