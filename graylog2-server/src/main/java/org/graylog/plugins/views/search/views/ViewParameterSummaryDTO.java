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
package org.graylog.plugins.views.search.views;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.Parameter;
import org.graylog.plugins.views.search.Search;

import java.util.Collection;

@AutoValue
@JsonAutoDetect
public abstract class ViewParameterSummaryDTO {
    public static final String FIELD_ID = "id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_SUMMARY = "summary";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_PARAMETERS = "parameters";

    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_TYPE)
    public abstract ViewDTO.Type type();

    @JsonProperty(FIELD_TITLE)
    public abstract String title();

    @JsonProperty(FIELD_SUMMARY)
    public abstract String summary();

    @JsonProperty(FIELD_DESCRIPTION)
    public abstract String description();

    @JsonProperty(FIELD_PARAMETERS)
    public abstract Collection<Parameter> parameters();

    public static ViewParameterSummaryDTO create(ViewDTO view, Search search) {
        return new AutoValue_ViewParameterSummaryDTO(view.id(), view.type(), view.title(), view.summary(), view.description(), search.parameters());
    }
}
