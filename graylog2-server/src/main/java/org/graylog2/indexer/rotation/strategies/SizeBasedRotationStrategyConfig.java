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
import com.github.joschi.jadconfig.util.Size;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;

import javax.validation.constraints.Min;

@JsonAutoDetect
@AutoValue
public abstract class SizeBasedRotationStrategyConfig implements RotationStrategyConfig {
    private static final long DEFAULT_MAX_SIZE = Size.gigabytes(1L).toBytes();

    @JsonProperty("max_size")
    public abstract long maxSize();

    @JsonCreator
    public static SizeBasedRotationStrategyConfig create(@JsonProperty(TYPE_FIELD) String type,
                                                         @JsonProperty("max_size") @Min(1) long maxSize) {
        return new AutoValue_SizeBasedRotationStrategyConfig(type, maxSize);
    }

    @JsonCreator
    public static SizeBasedRotationStrategyConfig create(@JsonProperty("max_size") @Min(1) long maxSize) {
        return create(SizeBasedRotationStrategyConfig.class.getCanonicalName(), maxSize);
    }

    public static SizeBasedRotationStrategyConfig defaultConfig() {
        return create(DEFAULT_MAX_SIZE);
    }
}
