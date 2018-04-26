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
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import org.graylog2.contentpacks.codecs.LookupTableCodec;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.lookup.db.DBLookupTableService;
import org.graylog2.lookup.dto.LookupTableDto;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class LookupTableCatalog implements EntityCatalog {
    public static final ModelType TYPE = ModelTypes.LOOKUP_TABLE;

    private final DBLookupTableService lookupTableService;
    private final LookupTableCodec codec;

    @Inject
    public LookupTableCatalog(DBLookupTableService lookupTableService,
                              LookupTableCodec codec) {
        this.lookupTableService = lookupTableService;
        this.codec = codec;
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return lookupTableService.findAll().stream()
                .map(codec::createExcerpt)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<Entity> collectEntity(EntityDescriptor entityDescriptor) {
        final ModelId modelId = entityDescriptor.id();
        return lookupTableService.get(modelId.id()).map(codec::encode);
    }

    @Override
    public Graph<EntityDescriptor> resolve(EntityDescriptor entityDescriptor) {
        final MutableGraph<EntityDescriptor> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entityDescriptor);

        final ModelId modelId = entityDescriptor.id();
        final Optional<LookupTableDto> lookupTableDto = lookupTableService.get(modelId.id());

        lookupTableDto.map(LookupTableDto::dataAdapterId)
                .map(dataAdapterId -> EntityDescriptor.create(ModelId.of(dataAdapterId), ModelTypes.LOOKUP_ADAPTER))
                .ifPresent(dataAdapter -> mutableGraph.putEdge(entityDescriptor, dataAdapter));
        lookupTableDto.map(LookupTableDto::cacheId)
                .map(cacheId -> EntityDescriptor.create(ModelId.of(cacheId), ModelTypes.LOOKUP_CACHE))
                .ifPresent(cache -> mutableGraph.putEdge(entityDescriptor, cache));

        return ImmutableGraph.copyOf(mutableGraph);
    }
}
