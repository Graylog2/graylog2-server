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

package org.graylog2.indexer.retention.strategies;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;

import javax.validation.constraints.Min;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class NoopRetentionStrategyConfig implements RetentionStrategyConfig {
    private static final int DEFAULT_MAX_NUMBER_OF_INDICES = Integer.MAX_VALUE;

    @JsonProperty("max_number_of_indices")
    public abstract int maxNumberOfIndices();

    @JsonCreator
    public static NoopRetentionStrategyConfig create(@JsonProperty(TYPE_FIELD) String type,
                                                     @JsonProperty("max_number_of_indices") @Min(1) int maxNumberOfIndices) {
        return new AutoValue_NoopRetentionStrategyConfig(type, maxNumberOfIndices);
    }

    @JsonCreator
    public static NoopRetentionStrategyConfig create(@JsonProperty("max_number_of_indices") @Min(1) int maxNumberOfIndices) {
        return new AutoValue_NoopRetentionStrategyConfig(NoopRetentionStrategyConfig.class.getCanonicalName(), maxNumberOfIndices);
    }

    public static NoopRetentionStrategyConfig createDefault() {
        return create(DEFAULT_MAX_NUMBER_OF_INDICES);
    }
}
