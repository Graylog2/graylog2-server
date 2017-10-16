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
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;

@JsonAutoDetect
@AutoValue
public abstract class NoopRotationStrategyConfig implements RotationStrategyConfig {
    @JsonCreator
    public static NoopRotationStrategyConfig create(@JsonProperty(TYPE_FIELD) String type) {
        return new AutoValue_NoopRotationStrategyConfig(type);
    }

    public static NoopRotationStrategyConfig createDefault() {
        return create(NoopRotationStrategyConfig.class.getCanonicalName());
    }
}
