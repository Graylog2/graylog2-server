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
package org.graylog2.rest.resources.search.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.rest.DetailedError;
import org.graylog2.plugin.rest.GenericError;

import javax.annotation.Nullable;
import java.util.Collection;

@JsonAutoDetect
@AutoValue
public abstract class QueryParseError implements DetailedError {
    @JsonProperty
    @Nullable
    public abstract Integer line();

    @JsonProperty
    @Nullable
    public abstract Integer column();


    public static QueryParseError create(String message,
                                         Collection<String> details,
                                         @Nullable Integer line,
                                         @Nullable Integer column) {
        return new AutoValue_QueryParseError(message, details, line, column);
    }
}
