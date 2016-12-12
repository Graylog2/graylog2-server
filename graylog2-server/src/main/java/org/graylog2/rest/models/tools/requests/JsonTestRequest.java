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
import org.graylog.autovalue.WithBeanGetter;
import org.hibernate.validator.constraints.NotEmpty;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class JsonTestRequest {
    @JsonProperty("flatten")
    public abstract boolean flatten();

    @JsonProperty("list_separator")
    @NotEmpty
    public abstract String listSeparator();

    @JsonProperty("key_separator")
    @NotEmpty
    public abstract String keySeparator();

    @JsonProperty("kv_separator")
    @NotEmpty
    public abstract String kvSeparator();

    @JsonProperty("replace_key_whitespace")
    public abstract boolean replaceKeyWhitespace();

    @JsonProperty("key_whitespace_replacement")
    public abstract String keyWhitespaceReplacement();

    @JsonProperty("key_prefix")
    public abstract String keyPrefix();

    @JsonProperty("string")
    @NotEmpty
    public abstract String string();

    @JsonCreator
    public static JsonTestRequest create(@JsonProperty("flatten") boolean flatten,
                                         @JsonProperty("list_separator") @NotEmpty String listSeparator,
                                         @JsonProperty("key_separator") @NotEmpty String keySeparator,
                                         @JsonProperty("kv_separator") @NotEmpty String kvSeparator,
                                         @JsonProperty("replace_key_whitespace") boolean replaceKeyWhitespace,
                                         @JsonProperty("key_whitespace_replacement") String keyWhitespaceReplacement,
                                         @JsonProperty("key_prefix") String keyPrefix,
                                         @JsonProperty("string") @NotEmpty String string) {
        return new AutoValue_JsonTestRequest(flatten, listSeparator, keySeparator, kvSeparator, replaceKeyWhitespace, keyWhitespaceReplacement, keyPrefix, string);
    }
}
