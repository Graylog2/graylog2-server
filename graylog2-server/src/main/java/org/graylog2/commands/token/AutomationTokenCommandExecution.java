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
package org.graylog2.commands.token;

import com.google.common.collect.Maps;
import com.mongodb.MongoException;
import jakarta.inject.Inject;
import java.util.Map;
import org.bson.types.ObjectId;
import org.graylog2.Configuration;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.security.AccessToken;
import org.graylog2.security.AccessTokenImpl;
import org.graylog2.security.AccessTokenService;

public class AutomationTokenCommandExecution {
    public static final String TOKEN_NAME = "Automation";
    public static final String TOKEN_ID = "000000000000000000000001";

    private final AccessTokenService accessTokenService;
    private final String rootUsername;

    @Inject
    public AutomationTokenCommandExecution(AccessTokenService accessTokenService, Configuration configuration) {
        this.accessTokenService = accessTokenService;
        this.rootUsername = configuration.getRootUsername();
    }

    public void run(String tokenValue) {
        AccessToken existingToken = accessTokenService.loadById(TOKEN_ID);
        if (existingToken != null && existingToken.getToken().equals(tokenValue)) {
            System.out.println("A automation token with name '" + existingToken.getName() +
                    "' and the provided value was already created by a previous run of this command. All good!");
            return;
        }

        AccessToken token = createToken(tokenValue);
        try {
            accessTokenService.save(token);
            System.out.println(
                    "Created/updated token with name '" + token.getName() + "' for user '" + rootUsername + "'.");
        } catch (MongoException e) {
            if (MongoUtils.isDuplicateKeyError(e)) {
                throw new RuntimeException("ERROR: Unable to add the token. This probably means that a token with the " +
                        "provided value is already present in the system but hasn't been created with this command. " +
                        "Please remove the offending token. Cause: " + e.getMessage() + ".", e);
            }
            throw e;
        } catch (ValidationException e) {
            throw new RuntimeException("ERROR: Unable to create a valid API token with the provided token value.", e);
        }
    }

    private AccessToken createToken(String tokenValue) {
        final Map<String, Object> fields = Maps.newHashMap();
        fields.put(AccessTokenImpl.TOKEN, tokenValue);
        fields.put(AccessTokenImpl.USERNAME, rootUsername);
        fields.put(AccessTokenImpl.NAME, TOKEN_NAME);
        fields.put(AccessTokenImpl.LAST_ACCESS, Tools.dateTimeFromDouble(0)); // aka never.
        return new AccessTokenImpl(new ObjectId(TOKEN_ID), fields);
    }
}
