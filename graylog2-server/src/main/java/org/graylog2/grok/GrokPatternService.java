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
package org.graylog2.grok;

import com.google.inject.ImplementedBy;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.ValidationException;

import java.util.Set;

@ImplementedBy(GrokPatternServiceImpl.class)
public interface GrokPatternService {
    GrokPattern load(String patternName) throws NotFoundException;

    Set<GrokPattern> loadAll();

    GrokPattern save(GrokPattern pattern) throws ValidationException;

    boolean validate(GrokPattern pattern);

    int delete(String patternName);
}
