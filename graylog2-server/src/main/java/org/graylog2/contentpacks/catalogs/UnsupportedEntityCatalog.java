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
package org.graylog2.contentpacks.catalogs;

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class UnsupportedEntityCatalog implements EntityCatalog {
    private static final Logger LOG = LoggerFactory.getLogger(UnsupportedEntityCatalog.class);

    public static final UnsupportedEntityCatalog INSTANCE = new UnsupportedEntityCatalog();

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return Collections.emptySet();
    }

    @Override
    public Optional<Entity> collectEntity(EntityDescriptor entityDescriptor) {
        LOG.warn("Couldn't collect entity {}", entityDescriptor);
        return Optional.empty();
    }

    @Override
    public Graph<EntityDescriptor> resolve(EntityDescriptor entityDescriptor) {
        LOG.warn("Couldn't resolve entity {}", entityDescriptor);
        return GraphBuilder.directed().build();
    }
}
