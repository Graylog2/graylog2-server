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

public class SearchTypeExporter {
    private final Defaults defaults;
    private final ExportBackend backend;

    @Inject
    public SearchTypeExporter(Defaults defaults, ExportBackend backend) {
        this.defaults = defaults;
        this.backend = backend;
    }

    public MessagesResult export(MessagesRequest request) {
        MessagesRequest fullRequest = defaults.fillInIfNecessary(request);
        ChunkedResult messages = backend.run(fullRequest);
        return resultFrom(messages);
    }

    public MessagesResult export(String searchId, String searchTypeId, SearchTypeOverrides overrides) {
        return resultFrom(null);
    }

    public MessagesResult resultFrom(ChunkedResult messages) {
        return MessagesResult.builder()
                .filename("affenmann.csv")
                .messages(messages)
                .build();
    }
}
