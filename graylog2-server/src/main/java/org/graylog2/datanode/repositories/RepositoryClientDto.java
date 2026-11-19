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
package org.graylog2.datanode.repositories;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import org.graylog2.database.MongoEntity;

/**
 * An opensearch repository client that Data Nodes configure, stored by the Graylog server. The repository is
 * registered in opensearch with this client. Data Nodes read these records when they build the opensearch
 * configuration, so new or changed clients take effect after the next opensearch restart.
 *
 * @param repositoryName the repository the client was created for, a repository has at most one client
 * @param clientName     the opensearch client name, see {@link RepositoryClientNames}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RepositoryClientDto(@Nullable String id,
                                  @JsonProperty(FIELD_REPOSITORY_NAME) String repositoryName,
                                  @JsonProperty(FIELD_CLIENT_NAME) String clientName,
                                  @JsonProperty(FIELD_OPENSEARCH) RepositoryClientSettings opensearch) implements MongoEntity {

    public static final String COLLECTION_NAME = "datanode_repository_clients";
    public static final String FIELD_REPOSITORY_NAME = "repository_name";
    public static final String FIELD_CLIENT_NAME = "client_name";
    public static final String FIELD_OPENSEARCH = "opensearch";
}
