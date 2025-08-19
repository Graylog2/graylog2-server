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

import com.google.common.collect.ImmutableMap;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.Configuration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.security.AccessTokenCipher;
import org.graylog2.security.AccessTokenImpl;
import org.graylog2.security.AccessTokenService;
import org.graylog2.security.AccessTokenServiceImpl;
import org.graylog2.security.PaginatedAccessTokenEntityService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.graylog2.commands.token.AutomationTokenCommandExecution.TOKEN_ID;
import static org.mockito.Mockito.doReturn;

public class AutomationTokenCommandExecutionTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private PaginatedAccessTokenEntityService paginatedAccessTokenEntityService;

    @Mock
    private ClusterConfigService configService;

    @Spy
    private Configuration configuration = new Configuration();

    private AccessTokenService accessTokenService;
    private AutomationTokenCommandExecution tokenCommandExecution;

    @Before
    public void setUp() {
        doReturn("password-secret").when(configuration)
                .getPasswordSecret();
        accessTokenService =
                new AccessTokenServiceImpl(mongodb.mongoConnection(), paginatedAccessTokenEntityService, new AccessTokenCipher(configuration), configService, configuration);
        tokenCommandExecution = new AutomationTokenCommandExecution(accessTokenService, configuration);
    }

    @Test
    public void createToken() {
        assertThat(accessTokenService.loadById(TOKEN_ID)).isNull();

        tokenCommandExecution.run("token");

        assertThat(accessTokenService.loadById(TOKEN_ID)).satisfies(token -> {
            assertThat(token).isNotNull();
            assertThat(token.getToken()).isEqualTo("token");
        });
    }

    @Test
    public void changeToken() {
        tokenCommandExecution.run("token");
        tokenCommandExecution.run("new-token");

        assertThat(accessTokenService.loadById(TOKEN_ID)).satisfies(token -> {
            assertThat(token).isNotNull();
            assertThat(token.getToken()).isEqualTo("new-token");
        });
    }

    @Test
    public void tokenAlreadyTaken() throws ValidationException {
        final AccessTokenImpl accessToken = new AccessTokenImpl(ObjectId.get(),
                ImmutableMap.of(AccessTokenImpl.TOKEN, "token",
                        AccessTokenImpl.NAME, "name",
                        AccessTokenImpl.USERNAME, "some-other-user"
                ));
        accessTokenService.save(accessToken);

        assertThatThrownBy(() -> tokenCommandExecution.run("token"))
                .hasMessageContaining("is already present")
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void tokenInvalid() {
        assertThatThrownBy(() -> tokenCommandExecution.run(""))
                .hasMessageContaining("Unable to create a valid API token")
                .isInstanceOf(RuntimeException.class);
    }
}
