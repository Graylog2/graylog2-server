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
package org.graylog2.bindings.providers;

import com.github.zafarkhaja.semver.Version;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Ping;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.indexer.gson.GsonUtils;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Optional;

public class ElasticsearchVersionProvider implements Provider<Version> {
    private final JestClient jestClient;

    @Inject
    public ElasticsearchVersionProvider(JestClient jestClient) {
        this.jestClient = jestClient;
    }

    @Override
    public Version get() {
        return getElasticsearchVersion(jestClient).orElseThrow(() -> new ElasticsearchException("Unable to retrieve Elasticsearch version"));
    }

    private Optional<Version> getElasticsearchVersion(JestClient jestClient) {
        final Ping request = new Ping.Builder().build();
        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Unable to retrieve Elasticsearch version");
        return Optional.ofNullable(jestResult.getJsonObject())
            .map(json -> GsonUtils.asJsonObject(json.get("version")))
            .map(json -> GsonUtils.asString(json.get("number")))
            .map(this::parseVersion);
    }

    private Version parseVersion(String version) {
        try {
            return Version.valueOf(version);
        } catch (Exception e) {
            throw new ElasticsearchException("Unable to parse Elasticsearch version: " + version, e);
        }
    }
}
