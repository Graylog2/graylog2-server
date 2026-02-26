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
package org.graylog2.database.grouping;

import jakarta.inject.Inject;
import org.apache.shiro.subject.Subject;
import org.graylog2.database.MongoConnection;
import org.graylog2.shared.security.EntityPermissionsUtils;
import org.graylog2.shared.users.UserService;

public class GrantsFirstFieldGroupingService implements EntityFieldGroupingService {

    private final MongoConnection mongoConnection;
    private final EntityPermissionsUtils permissionsUtils;
    private final UserService userService;

    @Inject
    public GrantsFirstFieldGroupingService(final MongoConnection mongoConnection,
                                           final EntityPermissionsUtils permissionsUtils,
                                           final UserService userService) {
        this.mongoConnection = mongoConnection;
        this.permissionsUtils = permissionsUtils;
        this.userService = userService;
    }

    @Override
    public EntityFieldBucketResponse groupByField(final String collectionName,
                                                  final String fieldName,
                                                  final String query,
                                                  final String bucketsFilter,
                                                  final int page,
                                                  final int pageSize,
                                                  final SortOrder sortOrder,
                                                  final SortField sortField,
                                                  final Subject subject) {
        //TODO: 1. convert subject to user
        //TODO: 2. Use userService.getPermissionsForUser() to get all user's permissions
        //TODO: 3. In those permissions, find IDs of entities that belong to collection `collectionName` which user is allowed to see
        //TODO: 4. Implement similar code to what is present in MongoEntityFieldGroupingService, but use the list of IDs as a additional filter, used on the same level as `query` filter

        return null;
    }


}
