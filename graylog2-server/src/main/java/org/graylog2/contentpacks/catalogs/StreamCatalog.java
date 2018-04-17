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

import org.graylog2.contentpacks.converters.StreamConverter;
import org.graylog2.contentpacks.converters.StreamExcerptConverter;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class StreamCatalog implements EntityCatalog {
    public static final ModelType TYPE = ModelTypes.STREAM;

    private final StreamService streamService;
    private final StreamExcerptConverter excerptConverter;
    private final StreamConverter converter;

    @Inject
    public StreamCatalog(StreamService streamService,
                         StreamExcerptConverter excerptConverter,
                         StreamConverter converter) {
        this.streamService = streamService;
        this.excerptConverter = excerptConverter;
        this.converter = converter;
    }

    @Override
    public boolean supports(ModelType modelType) {
        return TYPE.equals(modelType);
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return streamService.loadAll().stream()
                .map(excerptConverter::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<Entity> collectEntity(EntityDescriptor entityDescriptor) {
        final ModelId modelId = entityDescriptor.id();
        try {
            final Stream stream = streamService.load(modelId.id());
            return Optional.of(converter.convert(stream));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    public Set<EntityDescriptor> resolve(EntityDescriptor entityDescriptor) {
        final ModelId modelId = entityDescriptor.id();
        try {
            final Stream stream = streamService.load(modelId.id());
            return stream.getOutputs().stream()
                    .map(Output::getId)
                    .map(ModelId::of)
                    .map(id -> EntityDescriptor.create(id, ModelTypes.OUTPUT))
                    .collect(Collectors.toSet());
        } catch (NotFoundException e) {
            return Collections.emptySet();
        }
    }
}
