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
package org.graylog.events.processor;

import com.google.common.collect.ImmutableList;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.processor.storage.EventStorageHandler;
import org.graylog.events.processor.storage.PersistToStreamsStorageHandler;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class EventProcessorDtoTest {
    @Test
    public void automaticallyAddsPersistToStreamsStorageHandler() {
        final EventStorageHandler.Config testStorageHandlerConfig = new EventStorageHandler.Config() {
            @Override
            public String type() {
                return "storage-test";
            }
        };

        final PersistToStreamsStorageHandler.Config customPersistToStreams = PersistToStreamsStorageHandler.Config.builder()
                .streams(Collections.singletonList("abc"))
                .build();

        final EventDefinitionDto dto = EventDefinitionDto.builder()
                .title("Test")
                .description("Test")
                .priority(1)
                .config(new EventProcessorConfig.FallbackConfig())
                .keySpec(ImmutableList.of())
                .alert(false)
                .notificationSettings(EventNotificationSettings.withGracePeriod(60000))
                .storage(ImmutableList.of(customPersistToStreams, testStorageHandlerConfig))
                .build();

        // Make sure that any custom persist-to-streams handlers gets replaced by one with the default events stream.
        // Other handlers should not be touched.
        assertThat(dto.storage()).doesNotContain(customPersistToStreams);
        assertThat(dto.storage()).containsOnly(PersistToStreamsStorageHandler.Config.createWithDefaultEventsStream(), testStorageHandlerConfig);
    }
}