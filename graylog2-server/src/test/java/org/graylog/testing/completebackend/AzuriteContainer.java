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

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

public class AzuriteContainer extends GenericContainer<AzuriteContainer> {

    private static final String IMAGE_NAME = "mcr.microsoft.com/azure-storage/azurite:3.35.0";
    public static final int PORT = 10000;
    private final Network network;

    public static final String ACCOUNT_NAME = "devstoreaccount1";
    public static final String ACCOUNT_KEY = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";
    public static final String BLOB_HOST = ACCOUNT_NAME + ".blob.localhost";

    public AzuriteContainer() {
        super(IMAGE_NAME);
        this.network = Network.newNetwork();

        withNetwork(network);
        withNetworkAliases("azurite");
        withExposedPorts(PORT);
    }

    private String createConnectionString() {
        return "DefaultEndpointsProtocol=http;"
                + "AccountName=" + ACCOUNT_NAME + ";"
                + "AccountKey=" + ACCOUNT_KEY + ";"
                + "BlobEndpoint=" + getEndPoint() + ";";
    }

    public String getEndPoint() {
        return "http://" + BLOB_HOST + ":" + getMappedPort(PORT) ;
    }

    public BlobServiceClient createBlobServiceClient() {
        return new BlobServiceClientBuilder()
                .connectionString(createConnectionString())
                .buildClient();
    }

    public BlobServiceAsyncClient createBlobServiceAsyncClient() {
        return new BlobServiceClientBuilder()
                .connectionString(createConnectionString())
                .buildAsyncClient();
    }

    public BlobContainerClient createBlobContainer(String containerName) {
        BlobServiceClient client = createBlobServiceClient();
        client.createBlobContainer(containerName);
        return client.getBlobContainerClient(containerName);
    }

    @Override
    public void close() {
        super.close();
        if (network != null) {
            network.close();
        }
    }
}
