package org.graylog.plugins.map.config;

import com.google.cloud.NoCredentials;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

@Testcontainers
class GcsGeoIpFileServiceIT {
    @Container
    static final GenericContainer<?> fakeGcs = new GenericContainer<>("fsouza/fake-gcs-server")
            .withExposedPorts(4443)
            .withFileSystemBind("fake-gcs-data", "/data")
            .withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint(
                    "/bin/fake-gcs-server",
                    "-scheme", "http"
            ));
    private static Storage storageClient;

    @BeforeAll
    static void setUpFakeGcs() throws Exception {
        String fakeGcsExternalUrl = "http://" + fakeGcs.getHost() + ":" + fakeGcs.getFirstMappedPort();

        updateExternalUrlWithContainerUrl(fakeGcsExternalUrl);

        storageClient = StorageOptions.newBuilder()
                .setHost(fakeGcsExternalUrl)
                .setProjectId("test-project")
                .setCredentials(NoCredentials.getInstance())
                .build()
                .getService();
    }

    private static void updateExternalUrlWithContainerUrl(String fakeGcsExternalUrl) throws Exception {
        String modifyExternalUrlRequestUri = fakeGcsExternalUrl + "/_internal/config";
        String updateExternalUrlJson = "{"
                + "\"externalUrl\": \"" + fakeGcsExternalUrl + "\""
                + "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(modifyExternalUrlRequestUri))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(updateExternalUrlJson))
                .build();
        HttpResponse<Void> response = HttpClient.newBuilder().build()
                .send(req, HttpResponse.BodyHandlers.discarding());

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "error updating fake-gcs-server with external url, response status code " + response.statusCode() + " != 200");
        }
    }

    @Test
    void shouldUploadFileByWriterChannel() throws IOException {

        storageClient.create(BucketInfo.newBuilder("sample-bucket2").build());

        WriteChannel channel = storageClient.writer(BlobInfo.newBuilder("sample-bucket2", "some_file2.txt").build());
        channel.write(ByteBuffer.wrap("line1\n".getBytes(UTF_8)));
        channel.write(ByteBuffer.wrap("line2\n".getBytes(UTF_8)));
        channel.close();

        Blob someFile2 = storageClient.get("sample-bucket2", "some_file2.txt");
        String fileContent = new String(someFile2.getContent(), UTF_8);
        assertEquals("line1\nline2\n", fileContent);
    }
}
