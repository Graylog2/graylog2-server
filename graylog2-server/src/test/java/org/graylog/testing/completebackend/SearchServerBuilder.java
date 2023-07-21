package org.graylog.testing.completebackend;

import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog2.storage.SearchVersion;
import org.testcontainers.containers.Network;

import java.util.ArrayList;
import java.util.List;

public abstract class SearchServerBuilder<T extends SearchServerInstance> {
    public static final String DEFAULT_HEAP_SIZE = "2g";
    private final SearchVersion version;
    private Network network;
    private String heapSize = DEFAULT_HEAP_SIZE;
    private List<String> featureFlags = List.of();

    public SearchServerBuilder(final SearchVersion version) {
        this.version = version;
    }

    public SearchVersion getVersion() {
        return version;
    }

    public SearchServerBuilder network(final Network network){
        this.network = network;
        return this;
    }

    public Network getNetwork() {
        return network;
    }

    public SearchServerBuilder heapSize(final String heapSize) {
        this.heapSize = heapSize;
        return this;
    }

    public String getHeapSize() {
        return heapSize;
    }

    public SearchServerBuilder featureFlags(final List<String> featureFlags) {
        this.featureFlags = new ArrayList<>(featureFlags);
        return this;
    }

    public List<String> getFeatureFlags() {
        return featureFlags;
    }

    protected abstract T instantiate();

    public T build() {
        if(network == null) {
            network = Network.newNetwork();
        }
        return instantiate();
    }
}
