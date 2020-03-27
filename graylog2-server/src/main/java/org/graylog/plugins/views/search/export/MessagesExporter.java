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
package org.graylog.plugins.views.search.export;

import javax.inject.Inject;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public class MessagesExporter {
    private final Defaults defaults;
    private final ExportBackend backend;

    @Inject
    public MessagesExporter(Defaults defaults, ExportBackend backend) {
        this.defaults = defaults;
        this.backend = backend;
    }

    public void export(MessagesRequest request, ChunkForwarder<String> chunkForwarder) {
        MessagesRequest fullRequest = defaults.fillInIfNecessary(request);

        backend.run(fullRequest, h -> chunkForwarder.write(reduce(h)), chunkForwarder::close);
    }

    private String reduce(LinkedHashSet<LinkedHashSet<String>> hits) {
        return hits.stream()
                .map(row -> String.join(" ", row))
                .collect(Collectors.joining("\r\n"));
    }

    public void export(String searchId, String searchTypeId, SearchTypeOverrides overrides) {

    }
}
