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
package org.graylog.plugins.views.search.elasticsearch;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.engine.ESQueryDecorator;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Set;

public class ESQueryDecorators implements ESQueryDecorator {
    private final Set<ESQueryDecorator> queryDecorators;

    public static class Fake extends ESQueryDecorators {
        public Fake() {
            super(Collections.emptySet());
        }
    }

    @Inject
    public ESQueryDecorators(Set<ESQueryDecorator> queryDecorators) {
        this.queryDecorators = queryDecorators;
    }

    @Override
    public String decorate(String queryString, SearchJob job, Query query, Set<QueryResult> results) {
        return this.queryDecorators.isEmpty() ? queryString : this.queryDecorators.stream()
                .reduce(queryString, (prev, decorator) -> decorator.decorate(prev, job, query, results), String::concat);
    }
}
