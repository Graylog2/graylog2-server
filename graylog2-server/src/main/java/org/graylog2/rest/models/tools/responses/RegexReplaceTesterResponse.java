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
package org.graylog2.rest.models.tools.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class RegexReplaceTesterResponse {
    @JsonProperty
    public abstract boolean matched();

    @JsonProperty
    @Nullable
    public abstract Match match();

    @JsonProperty
    public abstract String regex();

    @JsonProperty
    public abstract String replacement();

    @JsonProperty("replace_all")
    public abstract boolean replaceAll();

    @JsonProperty
    public abstract String string();

    @JsonCreator
    public static RegexReplaceTesterResponse create(@JsonProperty("matched") boolean matched,
                                                    @JsonProperty("match") @Nullable Match match,
                                                    @JsonProperty("regex") String regex,
                                                    @JsonProperty("replacement") String replacement,
                                                    @JsonProperty("replace_all") boolean replaceAll,
                                                    @JsonProperty("string") String string) {
        return new AutoValue_RegexReplaceTesterResponse(matched, match, regex, replacement, replaceAll, string);
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class Match {
        @JsonProperty
        public abstract String match();

        @JsonProperty
        public abstract int start();

        @JsonProperty
        public abstract int end();

        @JsonCreator
        public static Match create(@JsonProperty("match") String match,
                                   @JsonProperty("start") int start,
                                   @JsonProperty("end") int end) {
            return new AutoValue_RegexReplaceTesterResponse_Match(match, start, end);
        }
    }
}
