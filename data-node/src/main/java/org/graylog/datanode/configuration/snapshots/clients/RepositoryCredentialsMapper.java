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
package org.graylog.datanode.configuration.snapshots.clients;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.graylog.datanode.configuration.snapshots.clients.RepositoryCredentialsDocument.Credentials;
import org.graylog.datanode.process.configuration.beans.OpensearchKeystoreFileContentItem;
import org.graylog.datanode.process.configuration.beans.OpensearchKeystoreItem;
import org.graylog.datanode.process.configuration.beans.OpensearchKeystoreStringItem;
import org.graylog2.datanode.repositories.AzureBlobEndpoints;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import software.amazon.awssdk.regions.PartitionMetadata;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Turns a warm tier repository credential record into the opensearch repository client the repository is registered
 * with. The Graylog server has to reject credentials that can't be converted for repositories with their own client,
 * so what is converted here and what the server accepts have to stay in sync.
 */
public class RepositoryCredentialsMapper {

    // Same fallback as the server-side S3 client uses
    private static final String DEFAULT_AWS_REGION = Region.US_EAST_1.id();
    // Value of the token_credential_type setting for the managed identity of the host
    private static final String AZURE_MANAGED_IDENTITY = "managed";

    private final EncryptedValueService encryptedValueService;

    @Inject
    public RepositoryCredentialsMapper(EncryptedValueService encryptedValueService) {
        this.encryptedValueService = encryptedValueService;
    }

    /**
     * The client of the given record.
     *
     * @param record a record with a client name and credentials
     * @throws IllegalArgumentException when opensearch can't use the credentials, the message tells why
     */
    public RepositoryClientConfiguration map(RepositoryCredentialsDocument record) {
        final Credentials credentials = record.credentials();
        if (credentials == null) {
            throw new IllegalArgumentException("the record has no credentials");
        }
        final Client client = new Client(credentials.typeOrDefault(), record.clientName());
        return switch (client.type()) {
            case RepositoryCredentialsDocument.TYPE_S3 -> s3(credentials, client);
            case RepositoryCredentialsDocument.TYPE_AZURE -> azure(credentials, client);
            case RepositoryCredentialsDocument.TYPE_GCS -> gcs(credentials, client);
            default -> throw new IllegalArgumentException(f("unknown credentials type %s", client.type()));
        };
    }

    private RepositoryClientConfiguration s3(Credentials credentials, Client client) {
        if (StringUtils.isNotBlank(credentials.awsExternalId())) {
            throw new IllegalArgumentException("an external ID for assuming an AWS role is not supported");
        }
        // Without keys, opensearch uses the default AWS credentials of the Data Node host
        if (!credentials.isAutomatic()) {
            if (StringUtils.isBlank(credentials.awsAccessKeyId())) {
                throw new IllegalArgumentException("an access key is required");
            }
            client.secure("access_key", credentials.awsAccessKeyId());
            client.secure("secret_key", decrypt(credentials.awsSecretAccessKey(), "secret key"));
        }
        if (StringUtils.isNotBlank(credentials.awsAssumeRoleArn())) {
            // The session name identifies the client, and so the repository, in the AWS CloudTrail logs. Client names
            // only contain characters AWS allows and are short enough.
            client.secure("role_arn", credentials.awsAssumeRoleArn());
            client.secure("role_session_name", f("graylog-%s", client.name()));
        }

        final String region = StringUtils.trimToNull(credentials.awsRegion());
        client.setting("region", region == null ? DEFAULT_AWS_REGION : region);
        final URI endpointUri = credentials.endpointUri();
        if (endpointUri != null) {
            // Custom S3 compatible storage, the server-side S3 client enforces path style access for these. Opensearch
            // passes the endpoint to the AWS SDK as an endpoint override, which keeps a path like the server does.
            // The URI isn't part of the message, it may contain user info.
            if (StringUtils.isBlank(endpointUri.getHost())) {
                throw new IllegalArgumentException("the endpoint has no host");
            }
            client.setting("protocol", endpointUri.getScheme());
            client.setting("endpoint", endpoint(endpointUri));
            client.setting("path_style_access", "true");
        } else {
            // AWS S3, opensearch requires an explicit endpoint
            client.setting("protocol", "https");
            client.setting("endpoint", region == null ? "s3.amazonaws.com" : f("s3.%s.%s", region, dnsSuffix(region)));
            client.setting("path_style_access", "false");
        }
        return client.configuration();
    }

    private RepositoryClientConfiguration azure(Credentials credentials, Client client) {
        final String accountName = credentials.accountName();
        if (StringUtils.isBlank(accountName)) {
            throw new IllegalArgumentException("an account name is required");
        }
        // Opensearch addresses https://<account>.blob.<endpoint suffix> only, without an endpoint the public cloud
        final URI endpointUri = credentials.endpointUri();
        if (endpointUri != null) {
            client.setting("endpoint_suffix", AzureBlobEndpoints.endpointSuffix(accountName, endpointUri)
                    .orElseThrow(() -> new IllegalArgumentException("the endpoint isn't https://<account>.blob.<endpoint suffix>")));
        }
        client.secure("account", accountName);
        if (credentials.isAutomatic()) {
            // Opensearch supports the managed identity of the Data Node host only, not other automatic credentials
            client.setting("token_credential_type", AZURE_MANAGED_IDENTITY);
        } else {
            client.secure("key", decrypt(credentials.accountKey(), "account key"));
        }
        return client.configuration();
    }

    private RepositoryClientConfiguration gcs(Credentials credentials, Client client) {
        // With automatic authentication, opensearch uses the Application Default Credentials of the Data Node host
        if (!credentials.isAutomatic()) {
            client.secureFile("credentials_file", decrypt(credentials.serviceAccountKey(), "service account key"));
        }
        return client.configuration();
    }

    private String decrypt(@Nullable EncryptedValue value, String name) {
        if (value == null || !value.isSet()) {
            throw new IllegalArgumentException(f("a %s is required", name));
        }
        final String decrypted = encryptedValueService.decrypt(value);
        if (StringUtils.isBlank(decrypted)) {
            throw new IllegalArgumentException(f("the %s can't be decrypted, make sure password_secret matches the Graylog server", name));
        }
        return decrypted;
    }

    // Without user info, it isn't used for authentication and must not end up in the opensearch configuration
    private static String endpoint(URI uri) {
        final String port = uri.getPort() == -1 ? "" : f(":%d", uri.getPort());
        final String rawPath = StringUtils.defaultString(uri.getRawPath());
        final String path = rawPath.endsWith("/") ? rawPath.substring(0, rawPath.length() - 1) : rawPath;
        return f("%s%s%s", uri.getHost(), port, path);
    }

    // Partitions like China (amazonaws.com.cn) or the ISO regions have their own domain, unknown regions fall back
    // to amazonaws.com
    private static String dnsSuffix(String region) {
        return PartitionMetadata.of(Region.of(region)).dnsSuffix();
    }

    /**
     * Collects the settings of a client under their full keys, e.g. {@code s3.client.<name>.endpoint}.
     */
    private record Client(String type, String name, Map<String, String> settings, List<OpensearchKeystoreItem> keystoreItems) {

        Client(String type, String name) {
            this(type, name, new LinkedHashMap<>(), new ArrayList<>());
        }

        void setting(String setting, String value) {
            settings.put(key(setting), value);
        }

        void secure(String setting, String value) {
            keystoreItems.add(new OpensearchKeystoreStringItem(key(setting), value));
        }

        void secureFile(String setting, String content) {
            keystoreItems.add(new OpensearchKeystoreFileContentItem(key(setting), content.getBytes(StandardCharsets.UTF_8)));
        }

        RepositoryClientConfiguration configuration() {
            return new RepositoryClientConfiguration(Map.copyOf(settings), List.copyOf(keystoreItems));
        }

        private String key(String setting) {
            return f("%s.client.%s.%s", type, name, setting);
        }
    }
}
