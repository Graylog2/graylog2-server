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

package org.graylog2.rest.models.tools.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@JsonAutoDetect
@AutoValue
public abstract class RegexReplaceTestRequest {
    @JsonProperty
    @NotNull
    public abstract String string();

    @JsonProperty
    @NotEmpty
    public abstract String regex();

    @JsonProperty
    @NotNull
    public abstract String replacement();

    @JsonProperty("replace_all")
    public abstract boolean replaceAll();

    @JsonCreator
    public static RegexReplaceTestRequest create(@JsonProperty("string") @NotNull String string,
                                                 @JsonProperty("regex") @NotEmpty String regex,
                                                 @JsonProperty("replacement") @NotNull String replacement,
                                                 @JsonProperty("replace_all") boolean replaceAll) {
        return new AutoValue_RegexReplaceTestRequest(string, regex, replacement, replaceAll);
    }
}
