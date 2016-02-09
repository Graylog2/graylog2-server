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

import javax.validation.constraints.Min;

@JsonAutoDetect
@AutoValue
public abstract class MessageCountRotationStrategyConfig implements RotationStrategyConfig {
    @JsonProperty("max_docs_per_index")
    public abstract int maxDocsPerIndex();

    @JsonCreator
    public static MessageCountRotationStrategyConfig create(@JsonProperty(TYPE_FIELD) String type,
                                                            @JsonProperty("max_docs_per_index") @Min(1) int maxDocsPerIndex) {
        return new AutoValue_MessageCountRotationStrategyConfig(type, maxDocsPerIndex);
    }

    @JsonCreator
    public static MessageCountRotationStrategyConfig create(@JsonProperty("max_docs_per_index") @Min(1) int maxDocsPerIndex) {
        return create(MessageCountRotationStrategyConfig.class.getCanonicalName(), maxDocsPerIndex);
    }

    @Override
    public DeflectorConfigResponse toDeflectorConfigResponse(int maxNumberOfIndices) {
        return MessageCountRotationStrategyResponse.create(maxNumberOfIndices, maxDocsPerIndex());
    }
}
