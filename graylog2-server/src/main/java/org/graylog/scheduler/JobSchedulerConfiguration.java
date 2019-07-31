package org.graylog.scheduler;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import org.graylog2.plugin.PluginConfigBean;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class JobSchedulerConfiguration implements PluginConfigBean {
    public static final String LOOP_SLEEP_DURATION = "job_scheduler_loop_sleep_duration";

    @Parameter(value = LOOP_SLEEP_DURATION, validators = PositiveDurationValidator.class)
    private Duration loopSleepDuration = Duration.seconds(1);

    public Duration getLoopSleepDuration() {
        return loopSleepDuration;
    }
}
