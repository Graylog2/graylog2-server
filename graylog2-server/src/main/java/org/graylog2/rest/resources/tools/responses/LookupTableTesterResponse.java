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
package org.graylog2.rest.resources.tools.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.lookup.LookupResult;

import javax.annotation.Nullable;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class LookupTableTesterResponse {
    @JsonProperty("empty")
    public abstract boolean empty();

    @JsonProperty("error")
    public abstract boolean error();

    @JsonProperty("error_message")
    public abstract String errorMessage();

    @JsonProperty("key")
    @Nullable
    public abstract Object key();

    @JsonProperty("value")
    @Nullable
    public abstract Object value();

    @JsonCreator
    public static LookupTableTesterResponse create(@JsonProperty("empty") boolean empty,
                                                   @JsonProperty("error") boolean error,
                                                   @JsonProperty("error_message") String errorMessage,
                                                   @JsonProperty("key") @Nullable Object key,
                                                   @JsonProperty("value") @Nullable Object value) {
        return new AutoValue_LookupTableTesterResponse(empty, error, errorMessage, key, value);
    }

    public static LookupTableTesterResponse error(String errorMessage) {
        return create(true, true, errorMessage, null, null);
    }

    public static LookupTableTesterResponse emptyResult(String string) {
        return create(true, false, "", string, null);
    }

    public static LookupTableTesterResponse result(String string, LookupResult result) {
        return create(result.isEmpty(), false, "", string, result.getSingleValue());
    }
}
