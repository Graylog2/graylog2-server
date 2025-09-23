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
package org.graylog2.tokenusage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.Assertions;
import org.bson.types.ObjectId;
import org.graylog.security.authservice.AuthServiceBackendConfig;
import org.graylog.security.authservice.AuthServiceBackendDTO;
import org.graylog.security.authservice.DBAuthServiceBackendService;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tokenusage.TokenUsageDTO;
import org.graylog2.search.SearchQuery;
import org.graylog2.security.AccessTokenEntity;
import org.graylog2.security.AccessTokenService;
import org.graylog2.security.PasswordAlgorithmFactory;
import org.graylog2.security.hashing.SHA1HashPasswordAlgorithm;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.UserImpl;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenUsageServiceImplTest {
    private static final int PAGE = 1;
    private static final int PER_PAGE = 10;
    private static final String SORT = AccessTokenEntity.FIELD_NAME;
    private static final SortOrder SORT_ORDER = SortOrder.ASCENDING;

    @Mock
    private AccessTokenService accessTokenService;
    @Mock
    private UserService userService;
    @Mock
    private DBAuthServiceBackendService dbAuthServiceBackendService;

    private TokenUsageServiceImpl testee;
    private UserFactory userFactory;
    private Object[] allMocks;

    @BeforeEach
    void setUp() {
        testee = new TokenUsageServiceImpl(accessTokenService, userService, dbAuthServiceBackendService);
        userFactory = new TokenUsageServiceImplTest.UserFactory(
                new Permissions(ImmutableSet.of(new RestPermissions())));
        allMocks = new Object[]{accessTokenService, userService, dbAuthServiceBackendService};
    }

    @Test
    void noAvailableTokensReturnEmptyResponse() {
        when(accessTokenService.findPaginated(any(SearchQuery.class), eq(PAGE), eq(PER_PAGE), eq(SORT), eq(SORT_ORDER)))
                .thenReturn(PaginatedList.emptyList(PAGE, PER_PAGE));
        when(dbAuthServiceBackendService.streamByIds(Collections.emptySet())).thenReturn(Stream.empty());
        final PaginatedList<TokenUsageDTO> expected = new PaginatedList<>(Collections.emptyList(), 0, PAGE, PER_PAGE);

        final PaginatedList<TokenUsageDTO> actual = testee.loadTokenUsage(PAGE, PER_PAGE, new SearchQuery(""), SORT, SORT_ORDER);
        assertThat(actual).isEqualTo(expected);

        verify(accessTokenService).findPaginated(any(SearchQuery.class), eq(PAGE), eq(PER_PAGE), eq(SORT), eq(SORT_ORDER));
        verify(dbAuthServiceBackendService).streamByIds(Collections.emptySet());
        verifyNoMoreInteractions(allMocks);
    }

    @Test
    void onlyLocalUsersDoesntHitTheAuthBackendService() {
        final AccessTokenEntity token1 = mkToken(1, Tools.nowUTC());
        final AccessTokenEntity token2 = mkToken(2, Tools.nowUTC());
        final User user1 = mkUser(1, false);
        final User user2 = mkUser(2, false);
        when(accessTokenService.findPaginated(any(SearchQuery.class), eq(PAGE), eq(PER_PAGE), eq(SORT), eq(SORT_ORDER)))
                .thenReturn(new PaginatedList<>(List.of(token1, token2), 2, PAGE, PER_PAGE));
        when(userService.load("userName1")).thenReturn(user1);
        when(userService.load("userName2")).thenReturn(user2);
        when(dbAuthServiceBackendService.streamByIds(Collections.emptySet())).thenReturn(Stream.empty());
        final PaginatedList<TokenUsageDTO> expected = new PaginatedList<>(List.of(mkTokenUsage(token1, user1, null, false), mkTokenUsage(token2, user2, null, false)), 2, PAGE, PER_PAGE);

        final PaginatedList<TokenUsageDTO> actual = testee.loadTokenUsage(PAGE, PER_PAGE, new SearchQuery(""), SORT, SORT_ORDER);
        assertThat(actual).isEqualTo(expected);

        verify(accessTokenService).findPaginated(any(SearchQuery.class), eq(PAGE), eq(PER_PAGE), eq(SORT), eq(SORT_ORDER));
        verify(userService).load("userName1");
        verify(userService).load("userName2");
        verify(dbAuthServiceBackendService).streamByIds(Collections.emptySet());
        verifyNoMoreInteractions(allMocks);
    }

    @Test
    void tokensFromExternalUsersShowAuthBackends() {
        final AccessTokenEntity token1 = mkToken(1, Tools.nowUTC());
        final AccessTokenEntity token2 = mkToken(2, Tools.dateTimeFromDouble(0));
        final User user1 = mkUser(1, true);
        final User user2 = mkUser(2, true);
        final AuthServiceBackendDTO authService1 = mkAuthService();
        when(accessTokenService.findPaginated(any(SearchQuery.class), eq(PAGE), eq(PER_PAGE), eq(SORT), eq(SORT_ORDER)))
                .thenReturn(new PaginatedList<>(List.of(token1, token2), 2, PAGE, PER_PAGE));
        when(userService.load("userName1")).thenReturn(user1);
        when(userService.load("userName2")).thenReturn(user2);
        when(dbAuthServiceBackendService.streamByIds(Set.of("auth-backend-id1", "auth-backend-id2"))).thenReturn(Stream.of(mkAuthService()));
        final PaginatedList<TokenUsageDTO> expected = new PaginatedList<>(List.of(mkTokenUsage(token1, user1, authService1.title(), false), mkTokenUsage(token2, user2, "<auth-backend-id2> (DELETED)", false)), 2, PAGE, PER_PAGE);

        final PaginatedList<TokenUsageDTO> actual = testee.loadTokenUsage(PAGE, PER_PAGE, new SearchQuery(""), SORT, SORT_ORDER);
        assertThat(actual).isEqualTo(expected);

        verify(accessTokenService).findPaginated(any(SearchQuery.class), eq(PAGE), eq(PER_PAGE), eq(SORT), eq(SORT_ORDER));
        verify(userService).load("userName1");
        verify(userService).load("userName2");
        verify(dbAuthServiceBackendService).streamByIds(Set.of("auth-backend-id1", "auth-backend-id2"));
        verifyNoMoreInteractions(allMocks);
    }

    @Test
    public void testLegacyToken() {
        final User user = mkUser(1, true);
        AccessTokenEntity token = AccessTokenEntity.Builder.create()
                .createdAt(null)
                .expiresAt(null)
                .lastAccess(Tools.nowUTC())
                .id("tokenId1234")
                .name("legacyToken")
                .userName(user.getName())
                .build();
        when(accessTokenService.findPaginated(any(SearchQuery.class), eq(PAGE), eq(PER_PAGE), eq(SORT), eq(SORT_ORDER)))
                .thenReturn(new PaginatedList<>(List.of(token), 1, PAGE, PER_PAGE));
        when(userService.load(user.getName())).thenReturn(user);

        testee.loadTokenUsage(PAGE, PER_PAGE, new SearchQuery(""), SORT, SORT_ORDER);
        Assertions.assertThatCode(() -> testee.loadTokenUsage(PAGE, PER_PAGE, new SearchQuery(""), SORT, SORT_ORDER))
                .doesNotThrowAnyException();
    }

    @Test
    void loadListWithDeletedUser() {
        final AccessTokenEntity token1 = mkToken(1, Tools.nowUTC());
        final AccessTokenEntity token2 = mkToken(2, Tools.nowUTC());
        final User user1 = mkUser(1, true);
        final User user2 = mkUser(2, true);
        final AuthServiceBackendDTO authService1 = mkAuthService();
        final SearchQuery query = new SearchQuery("");

        when(accessTokenService.findPaginated(any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(new PaginatedList<>(List.of(token1, token2), 2, PAGE, PER_PAGE));
        when(userService.load("userName1")).thenReturn(user1);
        when(userService.load("userName2")).thenReturn(null);
        when(dbAuthServiceBackendService.streamByIds(Set.of("auth-backend-id1"))).thenReturn(Stream.of(authService1));
        final PaginatedList<TokenUsageDTO> expected = new PaginatedList<>(List.of(mkTokenUsage(token1, user1, authService1.title(), false), mkTokenUsage(token2, user2, null, true)), 2, PAGE, PER_PAGE);

        final PaginatedList<TokenUsageDTO> actual = testee.loadTokenUsage(PAGE, PER_PAGE, query, AccessTokenEntity.FIELD_NAME, SortOrder.ASCENDING);

        assertThat(actual).isNotNull();
        assertThat(actual).isEqualTo(expected);

        verify(accessTokenService).findPaginated(query, PAGE, PER_PAGE, AccessTokenEntity.FIELD_NAME, SortOrder.ASCENDING);
        verify(userService).load(user1.getName());
        verify(userService).load(user2.getName());
        verify(dbAuthServiceBackendService).streamByIds(Set.of(authService1.id()));
        verifyNoMoreInteractions(allMocks);
    }

    //Just some helper methods


    private AccessTokenEntity mkToken(int number, DateTime lastAccess) {
        return AccessTokenEntity.Builder.create()
                .id("tokenId" + number)
                .name("tokenName" + number)
                .userName("userName" + number)
                .createdAt(Tools.nowUTC().minusDays(number))
                .lastAccess(lastAccess)
                .expiresAt(Tools.nowUTC().plusDays(number))
                .build();
    }

    private User mkUser(int number, boolean external) {
        final ObjectId id = new ObjectId(number, number);
        final Map<String, Object> fields = new HashMap<>(3);
        fields.put(UserImpl.EXTERNAL_USER, external);
        fields.put(UserImpl.USERNAME, "userName" + number);

        if (external) {
            fields.put(UserImpl.AUTH_SERVICE_ID, "auth-backend-id" + number);
        }
        return userFactory.create(id, fields);
    }

    private AuthServiceBackendDTO mkAuthService() {
        return AuthServiceBackendDTO.builder()
                .id("auth-backend-id1")
                .title("auth-backend-title1")
                .config(mock(AuthServiceBackendConfig.class))
                .build();
    }

    private TokenUsageDTO mkTokenUsage(AccessTokenEntity dto, User user, @Nullable String authBackendName, boolean isUserDeleted) {
        final String username = dto.userName();
        final boolean isExternal = user.isExternalUser();
        final String authBackend;
        if (user.getAuthServiceId() != null) {
            authBackend = Optional.ofNullable(authBackendName)
                    .orElse("<" + user.getAuthServiceId() + "> (DELETED)");
        } else {
            //User isn't associated with an auth-service:
            authBackend = "Internal";
        }

        return TokenUsageDTO.create(
                dto.id(),
                username,
                isUserDeleted ? null : user.getId(),
                dto.name(),
                dto.createdAt(),
                dto.lastAccess().getMillis() == 0 ? null : dto.lastAccess(), dto.expiresAt(),
                isUserDeleted ? false : isExternal,
                isUserDeleted ? "UNKNOWN" : authBackend,
                isUserDeleted
        );
    }

    public static class UserFactory implements UserImpl.Factory {
        private final Permissions permissions;
        private final PasswordAlgorithmFactory passwordAlgorithmFactory;
        private final ObjectMapper objectMapper;

        public UserFactory(Permissions permissions) {
            this.permissions = permissions;
            this.passwordAlgorithmFactory = new PasswordAlgorithmFactory(Collections.emptyMap(),
                    new SHA1HashPasswordAlgorithm("TESTSECRET"));
            this.objectMapper = new ObjectMapperProvider().get();
        }

        @Override
        public UserImpl create(Map<String, Object> fields) {
            return new UserImpl(passwordAlgorithmFactory, permissions, mock(ClusterConfigService.class), objectMapper, fields);
        }

        @Override
        public UserImpl create(ObjectId id, Map<String, Object> fields) {
            return new UserImpl(passwordAlgorithmFactory, permissions, mock(ClusterConfigService.class), objectMapper, id, fields);
        }

        // Not used.
        @Override
        public UserImpl.LocalAdminUser createLocalAdminUser(String adminRoleObjectId) {
            return null;
        }
    }

}
