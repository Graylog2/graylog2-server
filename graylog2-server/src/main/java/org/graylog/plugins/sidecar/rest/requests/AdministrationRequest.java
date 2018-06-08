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
package org.graylog.plugins.sidecar.rest.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

@AutoValue
@JsonAutoDetect
public abstract class AdministrationRequest {
    private final static int DEFAULT_PAGE = 1;
    private final static int DEFAULT_PER_PAGE = 50;

    @JsonProperty
    public abstract int page();

    @JsonProperty
    public abstract int perPage();

    @JsonProperty
    public abstract String query();

    @JsonProperty
    public abstract Map<String, String> filters();

    @JsonCreator
    public static AdministrationRequest create(@JsonProperty("page") int page,
                                               @JsonProperty("per_page") int perPage,
                                               @JsonProperty("query") @Nullable String query,
                                               @JsonProperty("filters") @Nullable Map<String, String> filters) {
        final int effectivePage = page == 0 ? DEFAULT_PAGE : page;
        final int effectivePerPage = perPage == 0 ? DEFAULT_PER_PAGE : perPage;
        return new AutoValue_AdministrationRequest(
                effectivePage,
                effectivePerPage,
                firstNonNull(query, ""),
                firstNonNull(filters, new HashMap<>()));
    }
}
