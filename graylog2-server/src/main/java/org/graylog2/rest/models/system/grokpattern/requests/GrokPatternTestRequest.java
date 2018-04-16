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
package org.graylog2.rest.models.system.grokpattern.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.grok.GrokPattern;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class GrokPatternTestRequest {

    @JsonProperty
    public abstract GrokPattern grokPattern();

    @JsonProperty
    public abstract String sampleData();

    @JsonCreator
    public static GrokPatternTestRequest create(@JsonProperty("grok_pattern") GrokPattern grokPattern,
                                         @JsonProperty("sample_data") String sampleData) {
       return new AutoValue_GrokPatternTestRequest(grokPattern, sampleData);
    }
}
