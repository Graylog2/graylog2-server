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

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.graylog.security.authservice.AuthServiceBackendDTO;
import org.graylog.security.authservice.DBAuthServiceBackendService;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.database.users.User;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tokenusage.TokenUsageDTO;
import org.graylog2.search.SearchQuery;
import org.graylog2.security.AccessTokenEntity;
import org.graylog2.security.AccessTokenService;
import org.graylog2.shared.tokenusage.TokenUsageService;
import org.graylog2.shared.users.UserService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TokenUsageServiceImpl implements TokenUsageService {
    private static final Logger LOG = LoggerFactory.getLogger(TokenUsageServiceImpl.class);

    private final AccessTokenService accessTokenService;
    private final UserService userService;
    private final DBAuthServiceBackendService dbAuthServiceBackendService;

    @Inject
    public TokenUsageServiceImpl(AccessTokenService accessTokenService, UserService userService, DBAuthServiceBackendService dbAuthServiceBackendService) {
        this.accessTokenService = accessTokenService;
        this.userService = userService;
        this.dbAuthServiceBackendService = dbAuthServiceBackendService;
    }

    @Override
    public PaginatedList<TokenUsageDTO> loadTokenUsage(int page,
                                                       int perPage,
                                                       SearchQuery searchQuery,
                                                       String sort,
                                                       SortOrder order) {
        final PaginatedList<AccessTokenEntity> currentPage = this.accessTokenService.findPaginated(searchQuery, page, perPage, sort, order);
        if (LOG.isInfoEnabled()) {
            final String logSearch = searchQuery.getQueryMap().isEmpty() ? "" : ", query \"" + searchQuery.getQueryString() + "\", sort by " + sort + ", ordering " + order;
            LOG.info("Loaded {} tokens in page {} containing max {} items{}.", currentPage.pagination().count(), page, perPage, logSearch);
        }

        //We loaded all matching tokens, let's now extract the respective users having created these tokens and (if applicable) their authentication-backend:
        final Map<String, User> usersOfThisPage = currentPage.stream()
                .map(AccessTokenEntity::userName)
                .distinct()
                .map(userService::load)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(User::getName, Function.identity()));
        LOG.debug("Found {} distinct users for current page of access tokens.", usersOfThisPage.size());

        //Collect all auth-service ids of the current page's users:
        final Set<String> allAuthServiceIds = usersOfThisPage.values().stream()
                .map(User::getAuthServiceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
        LOG.debug("Found {} distinct authentication services used by users of current page of access tokens.", allAuthServiceIds.size());

        //Load corresponding auth-services and extract the title:
        final Map<String, String> authServiceIdToTitle = dbAuthServiceBackendService.streamByIds(allAuthServiceIds)
                .collect(Collectors.toMap(AuthServiceBackendDTO::id, AuthServiceBackendDTO::title));

        //Build up the resulting objects:
        final List<TokenUsageDTO> tokenUsage = currentPage.stream()
                .map(usage -> toDTO(usage, usersOfThisPage, authServiceIdToTitle))
                .toList();

        return new PaginatedList<>(tokenUsage, currentPage.pagination().total(), page, perPage);

    }

    @Nonnull
    private TokenUsageDTO toDTO(AccessTokenEntity dto, Map<String, User> usersOfThisPage, Map<String, String> authServiceIdToTitle) {
        final String username = dto.userName();
        final User user = usersOfThisPage.get(username);
        final boolean isExternal = user.isExternalUser();
        final String authBackend;
        if (isExternal) {
            authBackend = Optional.ofNullable(authServiceIdToTitle.get(user.getAuthServiceId()))
                    .orElse("<" + user.getAuthServiceId() + "> (DELETED)");
        } else {
            //User is not external, so this field stays blank.
            authBackend = "";
        }

        //If the token was never accessed, we return null to make it more obvious in the frontend:
        final DateTime lastAccess = dto.lastAccess().getMillis() == 0 ? null : dto.lastAccess();

        return TokenUsageDTO.create(dto.id(), username, user.getId(), dto.name(), dto.createdAt(), lastAccess, isExternal, authBackend);
    }
}
