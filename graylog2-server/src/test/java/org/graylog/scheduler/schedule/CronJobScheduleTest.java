package org.graylog.scheduler.schedule;

import org.junit.jupiter.api.Test;

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
        assertThat(cronJobSchedule.timezone()).isEqualTo("UTC"); // default timezone
    }
}
