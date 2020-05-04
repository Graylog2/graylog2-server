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
package org.graylog.plugins.views.search.export.es;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.SearchScroll;
import org.graylog.plugins.views.search.export.ExportException;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.cluster.jest.JestUtils;

import javax.inject.Inject;
import java.util.Optional;
import java.util.function.Supplier;

import static org.graylog2.indexer.cluster.jest.JestUtils.checkForFailedShards;

public class JestWrapper {
    private final JestClient jestClient;

    @Inject
    public JestWrapper(JestClient jestClient) {
        this.jestClient = jestClient;
    }

    public SearchResult execute(Search search, Supplier<String> errorMessageSupplier) {
        SearchResult result = JestUtils.execute(jestClient, search, errorMessageSupplier);
        Optional<ElasticsearchException> elasticsearchException = checkForFailedShards(result);
        if (elasticsearchException.isPresent()) {
            throw new ExportException(errorMessageSupplier.get(), elasticsearchException.get());
        }
        return result;
    }

    public JestResult execute(SearchScroll scroll, Supplier<String> errorMessageSupplier) {
        final JestResult result = JestUtils.execute(jestClient, scroll, errorMessageSupplier);
        Optional<ElasticsearchException> elasticsearchException = checkForFailedShards(result);
        if (elasticsearchException.isPresent()) {
            throw new ExportException(errorMessageSupplier.get(), elasticsearchException.get());
        }
        return result;
    }
}
