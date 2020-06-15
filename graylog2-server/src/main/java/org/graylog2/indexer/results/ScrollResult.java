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
package org.graylog2.indexer.results;

import io.searchbox.core.SearchResult;

import java.io.IOException;
import java.util.List;

public interface ScrollResult {
    ScrollChunk nextChunk() throws IOException;

    String getQueryHash();

    long totalHits();

    void cancel() throws IOException;

    long tookMs();

    interface Factory {
        ScrollResult create(io.searchbox.core.SearchResult initialResult, String query, List<String> fields);
        ScrollResult create(SearchResult initialResult, String query, String scroll, List<String> fields);
    }

    interface ScrollChunk {
        List<String> getFields();

        int getChunkNumber();

        boolean isFirstChunk();

        List<ResultMessage> getMessages();
    }
}
