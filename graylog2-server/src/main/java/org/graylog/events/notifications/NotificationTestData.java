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

package org.graylog.events.notifications;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.events.contentpack.entities.EventProcessorConfigEntity;
import org.graylog.events.event.EventDto;
import org.graylog.events.event.EventOriginContext;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.EventProcessorConfig;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.rest.ValidationResult;
import org.graylog2.plugin.streams.Stream;

public class NotificationTestData {
    public static final String TEST_NOTIFICATION_ID = "this-is-a-test-notification";

    static EventNotificationContext getDummyContext(NotificationDto notificationDto, String userName) {
        final EventDto eventDto = EventDto.builder()
                .alert(true)
                .eventDefinitionId("EventDefinitionTestId")
                .eventDefinitionType("notification-test-v1")
                .eventTimestamp(Tools.nowUTC())
                .processingTimestamp(Tools.nowUTC())
                .id("NotificationTestId")
                .streams(ImmutableSet.of(Stream.DEFAULT_EVENTS_STREAM_ID))
                .message("Notification test message triggered from user <" + userName + ">")
                .source(Stream.DEFAULT_STREAM_ID)
                .keyTuple(ImmutableList.of("testkey"))
                .key("testkey")
                .originContext(EventOriginContext.elasticsearchMessage("testIndex_42", "b5e53442-12bb-4374-90ed-0deadbeefbaz"))
                .priority(2)
                .fields(ImmutableMap.of("field1", "value1", "field2", "value2"))
                .build();

        final EventDefinitionDto eventDefinitionDto = EventDefinitionDto.builder()
                .alert(true)
                .id(TEST_NOTIFICATION_ID)
                .title("Event Definition Test Title")
                .description("Event Definition Test Description")
                .config(new EventProcessorConfig() {
                    @Override
                    public String type() {
                        return "test-dummy-v1";
                    }
                    @Override
                    public ValidationResult validate() {
                        return null;
                    }
                    @Override
                    public EventProcessorConfigEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
                        return null;
                    }
                })
                .fieldSpec(ImmutableMap.of())
                .priority(2)
                .keySpec(ImmutableList.of())
                .notificationSettings(new EventNotificationSettings() {
                                          @Override
                                          public long gracePeriodMs() {
                                              return 0;
                                          }
                                          @Override
                                          // disable to avoid errors in getBacklogForEvent()
                                          public long backlogSize() {
                                              return 0;
                                          }
                                          @Override
                                          public Builder toBuilder() {
                                              return null;
                                          }
                                      }

                ).build();

        return EventNotificationContext.builder()
                .notificationId(TEST_NOTIFICATION_ID)
                .notificationConfig(notificationDto.config())
                .event(eventDto)
                .eventDefinition(eventDefinitionDto)
                .build();
    }
}
