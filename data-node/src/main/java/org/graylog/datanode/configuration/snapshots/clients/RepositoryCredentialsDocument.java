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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import org.graylog2.datanode.repositories.RepositoryClientNames;
import org.graylog2.security.encryption.EncryptedValue;

import java.net.URI;

/**
 * A warm tier repository credential record, as the enterprise plugin of the Graylog server stores it. Only the fields
 * needed for the repository client are mapped, credentials of all repository types are flattened into one record and
 * fields not used by a type stay {@code null}.
 *
 * @param clientName the opensearch client the repository is registered with, see
 *                   {@link RepositoryClientNames}. {@code null} for repositories whose
 *                   client Graylog doesn't manage, the Data Node doesn't configure a client for those.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RepositoryCredentialsDocument(@JsonProperty(FIELD_REPOSITORY_NAME) String repositoryName,
                                            @JsonProperty(FIELD_CLIENT_NAME) @Nullable String clientName,
                                            @JsonProperty("credentials") @Nullable Credentials credentials) {

    public static final String COLLECTION_NAME = "data_tiering_repository_credentials";
    public static final String FIELD_REPOSITORY_NAME = "repository_name";
    public static final String FIELD_CLIENT_NAME = "client_name";

    public static final String TYPE_S3 = "s3";
    public static final String TYPE_GCS = "gcs";
    public static final String TYPE_AZURE = "azure";

    private static final String AUTH_MODE_AUTOMATIC = "automatic";

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Credentials(@JsonProperty("type") @Nullable String type,
                              @JsonProperty("auth_mode") @Nullable String authMode,
                              @JsonProperty("aws_access_key_id") @Nullable String awsAccessKeyId,
                              @JsonProperty("aws_secret_access_key") @Nullable EncryptedValue awsSecretAccessKey,
                              @JsonProperty("aws_region") @Nullable String awsRegion,
                              @JsonProperty("endpoint_uri") @Nullable URI endpointUri,
                              @JsonProperty("aws_assume_role_arn") @Nullable String awsAssumeRoleArn,
                              @JsonProperty("aws_external_id") @Nullable String awsExternalId,
                              @JsonProperty("account_name") @Nullable String accountName,
                              @JsonProperty("account_key") @Nullable EncryptedValue accountKey,
                              @JsonProperty("service_account_key") @Nullable EncryptedValue serviceAccountKey) {

        /**
         * Records stored before other repository types got credentials have no type, they are S3 credentials, like
         * the server reads them.
         */
        public String typeOrDefault() {
            return type == null ? TYPE_S3 : type;
        }

        public boolean isAutomatic() {
            return AUTH_MODE_AUTOMATIC.equals(authMode);
        }
    }
}
