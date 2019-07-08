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
package org.graylog2.system.processing;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;

public class ProcessingStatusConfig {
    public static final String PERSIST_INTERVAL = "processing_status_persist_interval";

    @Parameter(value = PERSIST_INTERVAL, validators = {PositiveDurationValidator.class, Minimum1SecondValidator.class})
    private Duration processingStatusPersistInterval = Duration.seconds(1);

    public Duration getProcessingStatusPersistInterval() {
        return processingStatusPersistInterval;
    }

    public static class Minimum1SecondValidator implements Validator<Duration> {
        @Override
        public void validate(final String name, final Duration value) throws ValidationException {
            if (value != null && value.compareTo(Duration.seconds(1)) < 0) {
                throw new ValidationException("Parameter " + name + " should be at least 1 second (found " + value + ")");
            }
        }
    }
}
