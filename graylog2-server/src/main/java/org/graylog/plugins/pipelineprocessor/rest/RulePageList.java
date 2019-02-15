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
package org.graylog.plugins.pipelineprocessor.rest;

        import com.fasterxml.jackson.annotation.JsonAutoDetect;
        import com.fasterxml.jackson.annotation.JsonCreator;
        import com.fasterxml.jackson.annotation.JsonProperty;
        import com.google.auto.value.AutoValue;
        import org.graylog.autovalue.WithBeanGetter;
        import org.graylog2.database.PaginatedList;

        import javax.annotation.Nullable;
        import java.util.Collection;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class RulePageList {

    @Nullable
    @JsonProperty
    public abstract String query();

    @JsonProperty("pagination")
    public abstract PaginatedList.PaginationInfo paginationInfo();

    @JsonProperty
    public abstract long total();

    @Nullable
    @JsonProperty
    public abstract String sort();

    @Nullable
    @JsonProperty
    public abstract String order();

    @JsonProperty
    public abstract Collection<RuleSource> rules();

    @JsonCreator
    public static RulePageList create(
            @JsonProperty("query") @Nullable String query,
            @JsonProperty("pagination") PaginatedList.PaginationInfo paginationInfo,
            @JsonProperty("total") long total,
            @JsonProperty("sort") @Nullable String sort,
            @JsonProperty("order") @Nullable String order,
            @JsonProperty("rules") Collection<RuleSource> rules) {
        return new AutoValue_RulePageList(query, paginationInfo, total, sort, order, rules);
    }
}
