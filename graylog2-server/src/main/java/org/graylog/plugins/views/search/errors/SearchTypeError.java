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
package org.graylog.plugins.views.search.errors;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.views.search.Query;

import javax.annotation.Nonnull;

public class SearchTypeError extends QueryError {
    @Nonnull
    private final String searchTypeId;

    public SearchTypeError(@Nonnull Query query, @Nonnull String searchTypeId, Throwable throwable) {
        super(query, throwable);

        this.searchTypeId = searchTypeId;
    }

    public SearchTypeError(@Nonnull Query query, @Nonnull String searchTypeId, String description) {
        super(query, description);

        this.searchTypeId = searchTypeId;
    }

    @Nonnull
    @JsonProperty("search_type_id")
    public String searchTypeId() {
        return searchTypeId;
    }
}
