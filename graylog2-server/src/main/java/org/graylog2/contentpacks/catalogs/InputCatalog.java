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

import org.graylog2.contentpacks.converters.InputConverter;
import org.graylog2.contentpacks.converters.InputExcerptConverter;
import org.graylog2.contentpacks.converters.InputWithExtractors;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.inputs.InputService;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class InputCatalog implements EntityCatalog {
    private final InputService inputService;
    private final InputExcerptConverter excerptConverter;
    private final InputConverter converter;

    @Inject
    public InputCatalog(InputService inputService,
                        InputExcerptConverter excerptConverter,
                        InputConverter converter) {
        this.inputService = inputService;
        this.excerptConverter = excerptConverter;
        this.converter = converter;
    }

    @Override
    public boolean supports(ModelType modelType) {
        return ModelTypes.INPUT.equals(modelType);
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return inputService.all().stream()
                .map(excerptConverter::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Entity> collectEntities(Collection<ModelId> modelIds) {
        final Set<String> idStrings = modelIds.stream()
                .map(ModelId::id)
                .collect(Collectors.toSet());
        return inputService.findByIds(idStrings).stream()
                .map(input -> InputWithExtractors.create(input, inputService.getExtractors(input)))
                .map(converter::convert)
                .collect(Collectors.toSet());
    }
}
