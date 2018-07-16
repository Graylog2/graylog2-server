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
package org.graylog2.contentpacks.exceptions;

import org.graylog2.contentpacks.model.entities.Entity;

import java.util.Collection;

public class UnexpectedEntitiesException extends ContentPackException {
    private final Collection<Entity> unexpectedEntities;

    public UnexpectedEntitiesException(Collection<Entity> unexpectedEntities) {
        super("Unexpected entities in content pack: " + unexpectedEntities);
        this.unexpectedEntities = unexpectedEntities;
    }

    public Collection<Entity> getUnexpectedEntities() {
        return unexpectedEntities;
    }
}
