package org.graylog2.bindings.providers;

import com.github.zafarkhaja.semver.Version;
import io.searchbox.action.GenericResultAbstractAction;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Ping;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.IndexMapping2;
import org.graylog2.indexer.IndexMapping5;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.indexer.gson.GsonUtils;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Optional;

@Singleton
public class VersionSpecificIndexMappingProvider implements Provider<IndexMapping> {
    private final static Version V2 = Version.valueOf("2.0.0");
    private final static Version V5 = Version.valueOf("5.0.0");

    private final IndexMapping indexMapping;

    @Inject
    public VersionSpecificIndexMappingProvider(JestClientProvider jestClientProvider) {
        final JestClient jestClient = jestClientProvider.get();
        final Ping.Builder request = new Ping.Builder();

        final JestResult result = JestUtils.execute(jestClient, request.build(), () -> "Unable to retrieve node info.");

        final Optional<String> versionString = Optional.of(result.getJsonObject())
            .map(json -> GsonUtils.asJsonObject(json.get("version")))
            .map(versionObject -> GsonUtils.asString(versionObject.get("number")));

        final Version version = Version.valueOf(versionString.orElseThrow(() -> new ElasticsearchException("Unable to determine version from Elasticsearch cluster")));
        this.indexMapping = getIndexMappingForVersion(version);
    }

    @Override
    public IndexMapping get() {
        return this.indexMapping;
    }

    private IndexMapping getIndexMappingForVersion(Version version) {
        if (version.greaterThanOrEqualTo(V5)) {
            return new IndexMapping5();
        }
        if (version.greaterThanOrEqualTo(V2)) {
            return new IndexMapping2();
        }

        throw new ElasticsearchException("Elasticsearch cluster is using unsupported version: " + version.toString());
    }
}
