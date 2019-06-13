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

import org.graylog.plugins.views.Requirement;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchRequiresParameterSupport;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.Requirement;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchRequiresParameterSupport;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

public class RequiresParameterSupport implements Requirement<ViewDTO> {
    private final SearchDbService searchDbService;
    private final SearchRequiresParameterSupport searchRequiresParameterSupport;

    @Inject
    public RequiresParameterSupport(SearchDbService searchDbService, SearchRequiresParameterSupport searchRequiresParameterSupport) {
        this.searchDbService = searchDbService;
        this.searchRequiresParameterSupport = searchRequiresParameterSupport;
    }

    @Override
    public Map<String, PluginMetadataSummary> test(ViewDTO view) {
        final Optional<Search> optionalSearch = searchDbService.get(view.searchId());
        return optionalSearch.map(searchRequiresParameterSupport::test)
                .orElseThrow(() -> new IllegalStateException("Search " + view.searchId() + " for view " + view + " is missing."));
    }
}
