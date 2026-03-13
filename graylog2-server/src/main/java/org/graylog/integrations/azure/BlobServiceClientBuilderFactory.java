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
package org.graylog.integrations.azure;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Strings.isNullOrEmpty;

public class BlobServiceClientBuilderFactory {

    public static final String DEFAULT_BLOB_FQDN = "blob.core.windows.net";
    private static final Logger LOG = LoggerFactory.getLogger(BlobServiceClientBuilderFactory.class);

    private final EncryptedValueService encryptedValueService;

    @Inject
    public BlobServiceClientBuilderFactory(EncryptedValueService encryptedValueService) {
        this.encryptedValueService = encryptedValueService;
    }

    public BlobServiceClientBuilder create(String accountName,
                                           @Nullable EncryptedValue azureAccountKey,
                                           @Nullable String azureEndpoint) {
        BlobServiceClientBuilder clientBuilder = new BlobServiceClientBuilder();
        setCredentials(accountName, azureAccountKey, clientBuilder);
        setEndpoint(accountName, azureEndpoint, clientBuilder);

        return clientBuilder;
    }

    private void setCredentials(String accountName, @Nullable EncryptedValue azureAccountKey, BlobServiceClientBuilder clientBuilder) {
        if (azureAccountKey != null && azureAccountKey.isSet()) {
            final String accountKey = encryptedValueService.decrypt(azureAccountKey);
            if (accountKey == null || accountKey.isBlank()) {
                String errorMessage = "Azure account key is misconfigured: could not decrypt account key";
                LOG.error(errorMessage);
                throw new IllegalStateException(errorMessage);
            }
            clientBuilder.credential(new StorageSharedKeyCredential(accountName, accountKey));
        } else {
            clientBuilder.credential(new DefaultAzureCredentialBuilder().build());
        }
    }

    private void setEndpoint(String accountName, @Nullable String azureEndpoint, BlobServiceClientBuilder clientBuilder) {
        if (!isNullOrEmpty(azureEndpoint)) {
            clientBuilder.endpoint(azureEndpoint);
        } else {
            clientBuilder.endpoint("https://%s.%s".formatted(accountName, DEFAULT_BLOB_FQDN));
        }
    }
}
