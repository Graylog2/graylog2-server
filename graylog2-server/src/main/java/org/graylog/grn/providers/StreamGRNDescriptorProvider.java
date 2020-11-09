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

import org.graylog.grn.GRN;
import org.graylog.grn.GRNDescriptor;
import org.graylog.grn.GRNDescriptorProvider;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;

import javax.inject.Inject;

public class StreamGRNDescriptorProvider implements GRNDescriptorProvider {
    private final StreamService streamService;

    @Inject
    public StreamGRNDescriptorProvider(StreamService streamService) {
        this.streamService = streamService;
    }

    @Override
    public GRNDescriptor get(GRN grn) {
        try {
            final Stream stream = streamService.load(grn.entity());
            return GRNDescriptor.create(grn, stream.getTitle());
        } catch (NotFoundException e) {
            return GRNDescriptor.create(grn, "ERROR: Stream for <" + grn.toString() + "> not found!");
        }
    }
}
