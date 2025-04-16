package org.graylog.testing.completebackend;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.net.URI;

public class FakeGCSContainer extends GenericContainer<FakeGCSContainer> {
    private static final String IMAGE_NAME = "fsouza/fake-gcs-server";
    private static final int PORT = 4443;
    private final Network network;
    private final String projectId  = "graylog-project";

    public FakeGCSContainer() {
        super(IMAGE_NAME);
        this.network = Network.newNetwork();

        withNetwork(network);
        withNetworkAliases("gcp");
        withExposedPorts(PORT);

        withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint(
                "/bin/fake-gcs-server",
                "-scheme", "http"
        ));
    }

    public URI getEndpointUri() {
        return URI.create("http://" + getHost() + ":" + getMappedPort(PORT));
    }

    public Storage getStorage() {
        return StorageOptions.newBuilder()
                .setHost(getEndpointUri().toString())
                .setProjectId(projectId)
                .build()
                .getService();
    }

    public Bucket createBucket(String bucketName) {
        return getStorage().create(
                BucketInfo.newBuilder(bucketName)
                        .build());
    }

    public String getProjectId() {
        return projectId;
    }
}
