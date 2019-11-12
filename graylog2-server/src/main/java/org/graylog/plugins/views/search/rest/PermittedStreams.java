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
package org.graylog.plugins.views.search.rest;

import com.google.common.collect.ImmutableSet;
import org.graylog2.streams.StreamService;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toSet;
import static org.graylog2.plugin.streams.Stream.DEFAULT_EVENT_STREAM_IDS;

public class PermittedStreams {
    private final StreamService streamService;

    @Inject
    public PermittedStreams(StreamService streamService) {
        this.streamService = streamService;
    }

    public ImmutableSet<String> load(Predicate<String> isStreamIdPermitted) {
        final Set<String> result = streamService.loadAll().stream()
                .map(org.graylog2.plugin.streams.Stream::getId)
                // Unless explicitly queried, exclude event indices by default
                // Having the event indices in every search, makes sorting almost impossible
                // because it triggers https://github.com/Graylog2/graylog2-server/issues/6378
                // TODO: this filter can be removed, once we implement https://github.com/Graylog2/graylog2-server/issues/6490
                .filter(id -> !DEFAULT_EVENT_STREAM_IDS.contains(id))
                .filter(isStreamIdPermitted)
                .collect(toSet());

        if (result.isEmpty())
            throw new ForbiddenException("There are no streams you are permitted to use.");

        return ImmutableSet.copyOf(result);
    }
}
