/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.validation.constraints.Min;
import java.util.List;

@AutoValue
@JsonAutoDetect
public abstract class StageSource {
    @Min(0)
    @JsonProperty("stage")
    public abstract int stage();

    @JsonProperty("match_all")
    public abstract boolean matchAll();

    @JsonProperty("rules")
    public abstract List<String> rules();

    @JsonCreator
    public static StageSource create(@JsonProperty("stage") @Min(0) int stage,
                                     @JsonProperty("match_all") boolean matchAll,
                                     @JsonProperty("rules") List<String> rules) {
        return builder()
                .stage(stage)
                .matchAll(matchAll)
                .rules(rules)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_StageSource.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract StageSource build();

        public abstract Builder stage(int stageNumber);

        public abstract Builder matchAll(boolean mustMatchAll);

        public abstract Builder rules(List<String> ruleRefs);
    }
}
