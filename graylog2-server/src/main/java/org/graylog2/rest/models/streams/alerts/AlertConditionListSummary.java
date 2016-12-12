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
package org.graylog2.rest.models.streams.alerts;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.util.List;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class AlertConditionListSummary {
    @JsonProperty("total")
    public abstract long total();

    @JsonProperty("conditions")
    public abstract List<AlertConditionSummary> conditions();

    @JsonCreator
    public static AlertConditionListSummary create(@JsonProperty("total") long total,
                                                   @JsonProperty("conditions") List<AlertConditionSummary> conditions) {
        return new AutoValue_AlertConditionListSummary(total, conditions);
    }

    public static AlertConditionListSummary create(@JsonProperty("conditions") List<AlertConditionSummary> conditions) {
        return new AutoValue_AlertConditionListSummary(conditions.size(), conditions);
    }
}
