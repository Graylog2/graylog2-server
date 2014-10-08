/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.tools.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@JsonAutoDetect
@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class RegexTesterResponse {
    @JsonProperty
    public abstract boolean matched();

    @JsonProperty
    @Nullable
    public abstract Match match();

    @JsonProperty
    public abstract String regex();

    @JsonProperty
    public abstract String string();

    public static RegexTesterResponse create(boolean matched, @Nullable Match match, String regex, String string) {
        return new AutoValue_RegexTesterResponse(matched, match, regex, string);
    }

    @JsonAutoDetect
    @AutoValue
    public static abstract class Match {
        @JsonProperty
        public abstract String match();

        @JsonProperty
        public abstract int start();

        @JsonProperty
        public abstract int end();

        public static Match create(String match, int start, int end) {
            return new AutoValue_RegexTesterResponse_Match(match, start, end);
        }
    }
}
