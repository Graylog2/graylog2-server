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

import org.graylog2.contentpacks.codecs.DashboardCodec;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.dashboards.Dashboard;
import org.graylog2.dashboards.DashboardService;
import org.graylog2.database.NotFoundException;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DashboardCatalog implements EntityCatalog {
    public static final ModelType TYPE = ModelTypes.DASHBOARD;

    private final DashboardService dashboardService;
    private final DashboardCodec codec;

    @Inject
    public DashboardCatalog(DashboardService dashboardService,
                            DashboardCodec codec) {
        this.dashboardService = dashboardService;
        this.codec = codec;
    }

    @Override
    public boolean supports(ModelType modelType) {
        return TYPE.equals(modelType);
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return dashboardService.all().stream()
                .map(codec::createExcerpt)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<Entity> collectEntity(EntityDescriptor entityDescriptor) {
        final ModelId modelId = entityDescriptor.id();
        try {
            final Dashboard dashboard = dashboardService.load(modelId.id());
            return Optional.of(codec.encode(dashboard));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    public Set<EntityDescriptor> resolve(EntityDescriptor entityDescriptor) {
        return Collections.emptySet();
    }
}
