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
package org.graylog.plugins.views.search;

import com.google.inject.assistedinject.Assisted;
import org.graylog.plugins.views.Requirement;
import org.graylog.plugins.views.Requirements;

import javax.inject.Inject;
import java.util.Set;

public class SearchRequirements extends Requirements<Search> {
    @Inject
    public SearchRequirements(Set<Requirement<Search>> requirements, @Assisted Search dto) {
        super(requirements, dto);
    }

    public interface Factory extends Requirements.Factory<Search> {
        SearchRequirements create(Search dto);
    }
}
