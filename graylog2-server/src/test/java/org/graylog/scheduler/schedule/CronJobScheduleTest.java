/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.scheduler.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class CronJobScheduleTest {

    @Test
    void testCronExpressionValidationInvalid() {
        assertThatThrownBy(() -> CronJobSchedule.builder().cronExpression("nonsense").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cron expression contains 1 parts but we expect one of [6, 7]");
    }


    @Test
    void testCronExpressionValidationValid() {
        final CronJobSchedule cronJobSchedule = CronJobSchedule.builder().cronExpression("0 0 1 * * ? *").build();
        assertThat(cronJobSchedule.cronExpression()).isNotNull();
        assertThat(cronJobSchedule.timezone()).isNotPresent();
    }

    @Test
    void testNullTimezone() {
        final CronJobSchedule cronJobSchedule = CronJobSchedule.builder().cronExpression("0 0 1 * * ? *").timezone(null).build();
        assertThat(cronJobSchedule.timezone()).isNotPresent();
    }

    @Test
    void testDeserialize() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        objectMapper.registerSubtypes(new NamedType(CronJobSchedule.class, CronJobSchedule.TYPE_NAME));

        final CronJobSchedule schedule = objectMapper.readValue("{\"type\":\"cron\",\"cron_expression\":\"0 0 1 * * ? *\",\"timezone\":null}", CronJobSchedule.class);
        assertThat(schedule.type()).isEqualTo("cron");
        assertThat(schedule.cronExpression()).isEqualTo("0 0 1 * * ? *");
        assertThat(schedule.timezone()).isNotPresent();
    }
}
