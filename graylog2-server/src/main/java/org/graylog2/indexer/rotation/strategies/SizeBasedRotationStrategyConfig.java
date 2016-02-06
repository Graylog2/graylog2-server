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
import org.graylog2.rest.models.system.responses.SizeBasedRotationStrategyResponse;

@JsonAutoDetect
@AutoValue
public abstract class SizeBasedRotationStrategyConfig implements RotationStrategyConfig {
    @JsonProperty("max_size")
    public abstract long maxSize();

    @JsonCreator
    public static SizeBasedRotationStrategyConfig create(@JsonProperty(TYPE_FIELD) String type,
                                                         @JsonProperty("max_size") long maxSize) {
        return new AutoValue_SizeBasedRotationStrategyConfig(type, maxSize);
    }

    @JsonCreator
    public static SizeBasedRotationStrategyConfig create(@JsonProperty("max_size") long maxSize) {
        return create(SizeBasedRotationStrategyConfig.class.getCanonicalName(), maxSize);
    }

    @Override
    public DeflectorConfigResponse toDeflectorConfigResponse(int maxNumberOfIndices) {
        return SizeBasedRotationStrategyResponse.create(maxNumberOfIndices, maxSize());
    }
}
