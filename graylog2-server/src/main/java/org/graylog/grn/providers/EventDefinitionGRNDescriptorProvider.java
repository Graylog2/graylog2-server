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
package org.graylog.grn.providers;

import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNDescriptor;
import org.graylog.grn.GRNDescriptorProvider;

import javax.inject.Inject;
import java.util.Optional;

public class EventDefinitionGRNDescriptorProvider implements GRNDescriptorProvider {
    private final DBEventDefinitionService dbEventDefinitionService;

    @Inject
    public EventDefinitionGRNDescriptorProvider(DBEventDefinitionService dbEventDefinitionService) {
        this.dbEventDefinitionService = dbEventDefinitionService;
    }

    @Override
    public GRNDescriptor get(GRN grn) {
        final Optional<String> title = dbEventDefinitionService.get(grn.entity()).map(EventDefinitionDto::title);
        return GRNDescriptor.create(grn, title.orElse("ERROR: EventDefinition for <" + grn.toString() + "> not found!"));
    }
}
