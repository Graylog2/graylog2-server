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

import com.google.common.eventbus.EventBus;

import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.streams.StreamService;
import org.junit.Test;

import java.util.Collection;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class StreamCacheServiceTest {
    @Test
    @SuppressForbidden("Allow using default thread factory")
    public void getByName() throws Exception {
        final StreamCacheService streamCacheService = new StreamCacheService(new EventBus(), mock(StreamService.class), Executors.newSingleThreadScheduledExecutor());

        // make sure getByName always returns a collection
        final Collection<Stream> streams = streamCacheService.getByName("nonexisting");
        assertThat(streams).isNotNull().isEmpty();
    }

}
