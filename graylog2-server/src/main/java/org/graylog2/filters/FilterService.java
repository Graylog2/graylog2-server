/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.filters;

import org.graylog2.database.NotFoundException;
import org.graylog2.database.ValidationException;
import org.graylog2.filters.blacklist.FilterDescription;

import java.util.Set;

public interface FilterService {

    FilterDescription load(String filterId) throws NotFoundException;

    Set<FilterDescription> loadAll() throws NotFoundException;

    FilterDescription save(FilterDescription filter) throws ValidationException;

    boolean validate(FilterDescription filter);

    int delete(String filterId);
}
