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

public class ResultWindowLimitError extends SearchTypeError {

    private final int resultWindowLimit;

    ResultWindowLimitError(@Nonnull Query query, @Nonnull String searchTypeId, int resultWindowLimit, Throwable throwable) {
        super(query, searchTypeId, throwable);

        this.resultWindowLimit = resultWindowLimit;
    }

    @JsonProperty("result_window_limit")
    public Integer getResultWindowLimit() {
        return resultWindowLimit;
    }
}
