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
import org.graylog2.rest.models.tokenusage.TokenUsage;
import org.graylog2.rest.models.tokenusage.TokenUsageDTO;
import org.graylog2.search.SearchQuery;
import org.graylog2.shared.tokenusage.TokenUsageService;
import org.graylog2.shared.users.UserService;
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
    public static final Logger LOG = LoggerFactory.getLogger(TokenUsageServiceImpl.class);

    private final PaginatedTokenUsageService paginatedTokenUsageService;
    private final UserService userService;
    private final DBAuthServiceBackendService dbAuthServiceBackendService;

    @Inject
    public TokenUsageServiceImpl(PaginatedTokenUsageService paginatedTokenUsageService, UserService userService, DBAuthServiceBackendService dbAuthServiceBackendService) {
        this.paginatedTokenUsageService = paginatedTokenUsageService;
        this.userService = userService;
        this.dbAuthServiceBackendService = dbAuthServiceBackendService;
    }

    @Override
    public PaginatedList<TokenUsageDTO> loadTokenUsage(int page,
                                                       int perPage,
                                                       SearchQuery searchQuery,
                                                       String sort,
                                                       SortOrder order) {
        final PaginatedList<TokenUsage> currentPage = this.paginatedTokenUsageService.findPaginated(searchQuery, page, perPage, sort, order);

        //We loaded all matching tokens, let's now extract the respective users having created these tokens and (if applicable) their authentication-backend:
        final Map<String, User> usersOfThisPage = currentPage.stream()
                .map(TokenUsage::userName)
                .distinct()
                .map(userService::load)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(User::getName, Function.identity()));

        //Collect all auth-service ids of the current page's users:
        final Set<String> allAuthServiceIds = usersOfThisPage.values().stream()
                .map(User::getAuthServiceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());

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
    private TokenUsageDTO toDTO(TokenUsage dto, Map<String, User> usersOfThisPage, Map<String, String> authServiceIdToTitle) {
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

        return TokenUsageDTO.create(dto.id(), username, user.getId(), dto.name(), dto.createdAt(), dto.lastAccess(), isExternal, authBackend);
    }
}
