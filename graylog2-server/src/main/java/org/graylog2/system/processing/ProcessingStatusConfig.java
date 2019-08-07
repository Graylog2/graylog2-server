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
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class ProcessingStatusConfig {
    private static final String PREFIX = "processing_status_";
    static final String PERSIST_INTERVAL = PREFIX + "persist_interval";
    static final String UPDATE_THRESHOLD = PREFIX + "update_threshold";
    static final String JOURNAL_WRITE_RATE_THRESHOLD = PREFIX + "journal_write_rate_threshold";

    @Parameter(value = PERSIST_INTERVAL, validators = {PositiveDurationValidator.class, Minimum1SecondValidator.class})
    private Duration processingStatusPersistInterval = Duration.seconds(1);

    @Parameter(value = UPDATE_THRESHOLD, validators = {PositiveDurationValidator.class, Minimum1SecondValidator.class})
    private Duration updateThreshold = Duration.minutes(1);

    @Parameter(value = JOURNAL_WRITE_RATE_THRESHOLD, validators = PositiveIntegerValidator.class)
    private int journalWriteRateThreshold = 1;

    public Duration getProcessingStatusPersistInterval() {
        return processingStatusPersistInterval;
    }

    public Duration getUpdateThreshold() {
        return updateThreshold;
    }

    public int getJournalWriteRateThreshold() {
        return journalWriteRateThreshold;
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
