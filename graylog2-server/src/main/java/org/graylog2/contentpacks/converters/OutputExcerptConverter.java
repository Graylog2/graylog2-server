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
package org.graylog2.contentpacks.converters;

import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.plugin.streams.Output;

public class OutputExcerptConverter implements EntityExcerptConverter<Output> {
    @Override
    public EntityExcerpt convert(Output output) {
        return EntityExcerpt.builder()
                .id(ModelId.of(output.getId()))
                .type(ModelTypes.OUTPUT)
                .title(output.getTitle())
                .build();
    }
}
