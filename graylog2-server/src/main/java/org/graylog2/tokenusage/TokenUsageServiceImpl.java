package org.graylog2.tokenusage;

import com.google.inject.Inject;
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
import org.jetbrains.annotations.NotNull;
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
    public PaginatedList<TokenUsage> loadTokenUsage(int page,
                                                    int perPage,
                                                    SearchQuery searchQuery,
                                                    String sort,
                                                    SortOrder order) {
        final PaginatedList<TokenUsageDTO> currentPage = this.paginatedTokenUsageService.findPaginated(searchQuery, page, perPage, sort, order);

        //We loaded all matching tokens, let's now extract the respective users having created these tokens and (if applicable) their authentication-backend:
        final Map<String, User> usersOfThisPage = currentPage.stream()
                .map(TokenUsageDTO::userName)
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
        final List<TokenUsage> tokenUsage = currentPage.stream()
                .map(dto -> toTokenUsage(dto, usersOfThisPage, authServiceIdToTitle))
                .collect(Collectors.toUnmodifiableList());

        return new PaginatedList<>(tokenUsage, currentPage.pagination().total(), page, perPage);

    }

    private @NotNull TokenUsage toTokenUsage(TokenUsageDTO dto, Map<String, User> usersOfThisPage, Map<String, String> authServiceIdToTitle) {
        final String username = dto.userName();
        final User user = usersOfThisPage.get(username);
        final boolean isExternal = user.isExternalUser();
        final String authBackend;
        if (isExternal) {
            authBackend = Optional.ofNullable(authServiceIdToTitle.get(user.getAuthServiceId()))
                    .orElse("<" + user.getAuthServiceId() + "> (DELETED)");
        } else {
            //User is not external, so this field stays empty.
            authBackend = "";
        }

        return TokenUsage.create(username, dto.name(), dto.createdAt(), dto.lastAccess(), isExternal, authBackend);
    }
}
