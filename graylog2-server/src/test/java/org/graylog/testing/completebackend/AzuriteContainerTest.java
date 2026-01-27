/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.testing.completebackend;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AzuriteContainerTest {
    private AzuriteContainer azurite;

    @BeforeAll
    void setUp() {
        azurite = new AzuriteContainer();
        azurite.start();
    }

    @AfterAll
    void tearDown() {
        if (azurite != null) {
            azurite.close();
        }
    }

    @Test
    void containerShouldBeRunning() {
        assertTrue(azurite.isRunning());
    }

    @Test
    void shouldCreateBlobContainer() {
        String containerName = "test-container-" + System.currentTimeMillis();

        BlobContainerClient container = azurite.createBlobContainer(containerName);

        assertNotNull(container);
        assertTrue(container.exists());
        assertEquals(containerName, container.getBlobContainerName());
    }

    @Test
    void shouldUploadAndDownloadBlob() {
        String containerName = "upload-test-" + System.currentTimeMillis();
        String blobName = "test-blob.txt";
        String content = "Hello, Azurite!";

        BlobContainerClient container = azurite.createBlobContainer(containerName);
        BlobClient blobClient = container.getBlobClient(blobName);

        // Upload
        blobClient.upload(BinaryData.fromString(content));

        // Download
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blobClient.downloadStream(outputStream);
        String downloaded = outputStream.toString(StandardCharsets.UTF_8);

        assertEquals(content, downloaded);
    }

    @Test
    void shouldListBlobsInContainer() {
        String containerName = "list-test-" + System.currentTimeMillis();
        BlobContainerClient container = azurite.createBlobContainer(containerName);

        // Upload
        container.getBlobClient("file1.txt").upload(BinaryData.fromString("content1"));
        container.getBlobClient("file2.txt").upload(BinaryData.fromString("content2"));

        List<String> blobNames = container.listBlobs().stream()
                .map(BlobItem::getName)
                .collect(Collectors.toList());

        assertEquals(2, blobNames.size());
        assertTrue(blobNames.contains("file1.txt"));
        assertTrue(blobNames.contains("file2.txt"));
    }
}
