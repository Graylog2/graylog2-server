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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchTypeError extends QueryError {
    @Nonnull
    private final String searchTypeId;

    private final Integer resultWindowLimit;

    public SearchTypeError(@Nonnull Query query, @Nonnull String searchTypeId, Throwable throwable) {
        super(query, throwable);

        this.resultWindowLimit = parseResultLimit(throwable);

        this.searchTypeId = searchTypeId;
    }

    public SearchTypeError(@Nonnull Query query, @Nonnull String searchTypeId, String description) {
        super(query, description);

        this.resultWindowLimit = parseResultLimit(description);

        this.searchTypeId = searchTypeId;
    }

    private Integer parseResultLimit(Throwable throwable) {
        return parseResultLimit(throwable.getMessage());
    }

    private Integer parseResultLimit(String description) {
        if (description.toLowerCase().contains("result window is too large")) {
            final Matcher matcher = Pattern.compile("[0-9]+").matcher(description);
            if (matcher.find())
                return Integer.parseInt(matcher.group(0));
        }
        return null;
    }

    @Nonnull
    @JsonProperty("search_type_id")
    public String searchTypeId() {
        return searchTypeId;
    }

    @JsonProperty("result_window_limit")
    public Integer getResultWindowLimit() {
        return resultWindowLimit;
    }
}
