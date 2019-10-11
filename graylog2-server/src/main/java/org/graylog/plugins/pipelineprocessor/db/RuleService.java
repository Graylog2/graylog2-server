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
package org.graylog.plugins.pipelineprocessor.db;

import org.graylog2.database.NotFoundException;

import java.util.Collection;
import java.util.Optional;

public interface RuleService {
    RuleDao save(RuleDao rule);

    RuleDao load(String id) throws NotFoundException;

    RuleDao loadByName(String name) throws NotFoundException;

    default Optional<RuleDao> findByName(String name) {
        try {
           RuleDao ruleDao = this.loadByName(name);
           return Optional.of(ruleDao);
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    Collection<RuleDao> loadAll();

    void delete(String id);

    Collection<RuleDao> loadNamed(Collection<String> ruleNames);
}
