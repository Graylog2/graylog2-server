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

package org.graylog2.indexer.rotation.strategies;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.rest.models.system.responses.DeflectorConfigResponse;
import org.graylog2.rest.models.system.responses.MessageCountRotationStrategyResponse;
import org.graylog2.rest.models.system.responses.TimeBasedRotationStrategyResponse;
import org.joda.time.Period;

@JsonAutoDetect
@AutoValue
public abstract class TimeBasedRotationStrategyConfig implements RotationStrategyConfig {
    @JsonProperty("rotation_period")
    public abstract Period rotationPeriod();

    @JsonCreator
    public static TimeBasedRotationStrategyConfig create(@JsonProperty(TYPE_FIELD) String type,
                                                         @JsonProperty("rotation_period") Period maxTimePerIndex) {
        return new AutoValue_TimeBasedRotationStrategyConfig(type, maxTimePerIndex);
    }

    @JsonCreator
    public static TimeBasedRotationStrategyConfig create(@JsonProperty("rotation_period") Period maxTimePerIndex) {
        return create(TimeBasedRotationStrategyConfig.class.getCanonicalName(), maxTimePerIndex);
    }

    @Override
    public DeflectorConfigResponse toDeflectorConfigResponse(int maxNumberOfIndices) {
        return TimeBasedRotationStrategyResponse.create(maxNumberOfIndices, rotationPeriod());
    }
}
