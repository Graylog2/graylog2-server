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
package org.graylog.scheduler;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import org.graylog2.plugin.PluginConfigBean;

/**
 * Job scheduler specific configuration fields for the server configuration file.
 */
@SuppressWarnings({"FieldCanBeLocal", "unused", "WeakerAccess"})
public class JobSchedulerConfiguration implements PluginConfigBean {
    public static final String LOOP_SLEEP_DURATION = "job_scheduler_loop_sleep_duration";

    @Parameter(value = LOOP_SLEEP_DURATION, validators = PositiveDurationValidator.class)
    private Duration loopSleepDuration = Duration.seconds(1);

    public Duration getLoopSleepDuration() {
        return loopSleepDuration;
    }
}
