package org.graylog.storage.opensearch2;

import com.google.common.eventbus.EventBus;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.graylog.shaded.opensearch2.org.opensearch.common.xcontent.json.JsonXContent;
import org.graylog.shaded.opensearch2.org.opensearch.core.common.bytes.BytesReference;
import org.graylog.shaded.opensearch2.org.opensearch.core.xcontent.ToXContent;
import org.graylog.shaded.opensearch2.org.opensearch.core.xcontent.XContentBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.index.reindex.ReindexRequest;
import org.graylog.shaded.opensearch2.org.opensearch.index.reindex.RemoteInfo;
import org.graylog2.datanode.RemoteReindexAllowlistEvent;
import org.graylog2.indexer.datanode.RemoteReindexingMigrationAdapter;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders.matchAllQuery;

public class RemoteReindexingMigrationAdapterOS2 implements RemoteReindexingMigrationAdapter {
    private final OpenSearchClient client;
    private final OkHttpClient httpClient;
    private final EventBus eventBus;

    @Inject
    public RemoteReindexingMigrationAdapterOS2(final OpenSearchClient client,
                                               final OkHttpClient httpClient,
                                               final EventBus eventBus) {
        this.client = client;
        this.httpClient = httpClient;
        this.eventBus = eventBus;
    }

    @Override
    public void start(final String host, final String username, final String password, final List<String> indices) {
        var toReindex = isAllIndices(indices) ? getAllIndicesFrom(host, username, password) : indices;

        allowReindexingFrom(host);
        // TODO: wait for cluster to come back up before
        reindex(host, username, password, toReindex);
        removeAllowlist(host);
    }

    private void removeAllowlist(final String host) {
        eventBus.post(new RemoteReindexAllowlistEvent(host, RemoteReindexAllowlistEvent.ACTION.REMOVE));
    }

    private void allowReindexingFrom(final String host) {
        eventBus.post(new RemoteReindexAllowlistEvent(host, RemoteReindexAllowlistEvent.ACTION.ADD));
    }

    private List<String> getAllIndicesFrom(final String host, final String username, final String password) {
        var url = (host.endsWith("/") ? host : host + "/") + "_cat/indices?h=index";
        try (var response = httpClient.newCall(new Request.Builder().url(url).header("Authorization", Credentials.basic(username, password)).build()).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return new BufferedReader(new StringReader(response.body().string())).lines().toList();
            } else {
                throw new RuntimeException("Could not read list of indices from " + host);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read list of indices from " + host + ", " + e.getMessage(), e);
        }
    }

    private boolean isAllIndices(final List<String> indices) {
        return indices.isEmpty() || (indices.size() == 1 && "*".equals(indices.get(0)));
    }

    private void reindex(final String host, final String username, final String password, final List<String> indices) {
        try (XContentBuilder builder = JsonXContent.contentBuilder().prettyPrint()) {
            BytesReference query = BytesReference.bytes(matchAllQuery().toXContent(builder, ToXContent.EMPTY_PARAMS));

            URI uri = new URI(host);

            for (var index : indices) {
                final ReindexRequest reindexRequest = new ReindexRequest();
                reindexRequest
                        .setRemoteInfo(new RemoteInfo(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(), query, username, password, Map.of(), RemoteInfo.DEFAULT_SOCKET_TIMEOUT, RemoteInfo.DEFAULT_CONNECT_TIMEOUT))
                        .setSourceIndices(index).setDestIndex(index);

                var reindexResult = client.execute((c, requestOptions) -> c.reindex(reindexRequest, requestOptions));
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Status status() {
        return null;
    }
}
