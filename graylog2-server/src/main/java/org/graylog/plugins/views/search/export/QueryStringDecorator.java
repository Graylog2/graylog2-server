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
package org.graylog.plugins.views.search.export;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.elasticsearch.ESQueryDecorators;

import javax.inject.Inject;
import java.util.UUID;

public class QueryStringDecorator {

    private final ESQueryDecorators decorator;

    @Inject
    public QueryStringDecorator(ESQueryDecorators decorator) {
        this.decorator = decorator;
    }

    public String decorateQueryString(String queryString, Search search, Query query) {

        SearchJob jobStub = new SearchJob(UUID.randomUUID().toString(), search, "views backend");

        return decorator.decorate(queryString, jobStub, query, ImmutableSet.of());
    }
}
