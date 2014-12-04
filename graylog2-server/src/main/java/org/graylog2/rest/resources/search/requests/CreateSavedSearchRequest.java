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
package org.graylog2.rest.resources.search.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.Map;

@JsonAutoDetect
@AutoValue
public abstract class CreateSavedSearchRequest {
    @JsonProperty
    public abstract String title();

    @JsonProperty
    public abstract Map<String, Object> query();

    @JsonCreator
    public static CreateSavedSearchRequest create(@JsonProperty("title") @NotEmpty String title,
                                                  @JsonProperty("query") @NotEmpty Map<String, Object> query) {
        return new AutoValue_CreateSavedSearchRequest(title, query);
    }
}
