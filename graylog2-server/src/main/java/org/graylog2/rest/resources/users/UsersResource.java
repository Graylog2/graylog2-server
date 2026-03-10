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
package org.graylog2.rest.resources.users;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.security.UserContext;
import org.graylog.security.authservice.AuthServiceBackendDTO;
import org.graylog.security.authservice.GlobalAuthServiceConfig;
import org.graylog.security.permissions.CaseSensitiveWildcardPermission;
import org.graylog.security.permissions.GRNPermission;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.users.requests.ChangePasswordRequest;
import org.graylog2.rest.models.users.requests.CreateUserRequest;
import org.graylog2.rest.models.users.requests.PermissionEditRequest;
import org.graylog2.rest.models.users.requests.Startpage;
import org.graylog2.rest.models.users.requests.UpdateUserPreferences;
import org.graylog2.rest.models.users.responses.BasicUserResponse;
import org.graylog2.rest.models.users.responses.Token;
import org.graylog2.rest.models.users.responses.TokenList;
import org.graylog2.rest.models.users.responses.TokenSummary;
import org.graylog2.rest.models.users.responses.UserList;
import org.graylog2.rest.models.users.responses.UserSummary;
import org.graylog2.rest.models.users.responses.UsernameAvailabilityResponse;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.security.AccessToken;
import org.graylog2.security.AccessTokenService;
import org.graylog2.security.UserSessionTerminationService;
import org.graylog2.security.sessions.SessionDTO;
import org.graylog2.security.sessions.SessionService;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.ChangeUserRequest;
import org.graylog2.shared.users.Role;
import org.graylog2.shared.users.UserManagementService;
import org.graylog2.users.PaginatedUserService;
import org.graylog2.users.PasswordComplexityConfig;
import org.graylog2.users.RoleService;
import org.graylog2.users.RoleServiceImpl;
import org.graylog2.users.UserConfiguration;
import org.graylog2.users.UserOverviewDTO;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.PeriodDuration;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.maxBy;
import static org.graylog2.shared.security.RestPermissions.USERS_EDIT;
import static org.graylog2.shared.security.RestPermissions.USERS_PERMISSIONSEDIT;
import static org.graylog2.shared.security.RestPermissions.USERS_READ;
import static org.graylog2.shared.security.RestPermissions.USERS_ROLESEDIT;
import static org.graylog2.shared.security.RestPermissions.USERS_TOKENCREATE;
import static org.graylog2.shared.security.RestPermissions.USERS_TOKENLIST;
import static org.graylog2.shared.security.RestPermissions.USERS_TOKENREMOVE;
import static org.graylog2.users.PasswordComplexityConfig.SPECIAL_CHARACTERS;
import static org.graylog2.users.PasswordComplexityConfig.SPECIAL_CHARACTERS_CODEPOINTS;

@RequiresAuthentication
@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PublicCloudAPI
@Tag(name = "Users", description = "User accounts")
public class UsersResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(UsersResource.class);
    private static final int USERNAME_PERMISSION_PARTS_LENGTH = 3;
    private static final String USER_PERMISSION_DOMAIN = "users";

    private final UserManagementService userManagementService;
    private final PaginatedUserService paginatedUserService;
    private final AccessTokenService accessTokenService;
    private final RoleService roleService;
    private final SessionService sessionService;
    private final SearchQueryParser searchQueryParser;
    private final UserSessionTerminationService sessionTerminationService;
    private final DefaultSecurityManager securityManager;
    private final GlobalAuthServiceConfig globalAuthServiceConfig;
    private final ClusterConfigService clusterConfigService;
    private final AuditEventSender auditEventSender;

    protected static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put(UserOverviewDTO.FIELD_ID, SearchQueryField.create("_id", SearchQueryField.Type.OBJECT_ID))
            .put(UserOverviewDTO.FIELD_USERNAME, SearchQueryField.create(UserOverviewDTO.FIELD_USERNAME))
            .put(UserOverviewDTO.FIELD_FULL_NAME, SearchQueryField.create(UserOverviewDTO.FIELD_FULL_NAME))
            .put(UserOverviewDTO.FIELD_EMAIL, SearchQueryField.create(UserOverviewDTO.FIELD_EMAIL))
            .build();

    @Inject
    public UsersResource(UserManagementService userManagementService,
                         PaginatedUserService paginatedUserService,
                         AccessTokenService accessTokenService,
                         RoleService roleService,
                         SessionService sessionService,
                         UserSessionTerminationService sessionTerminationService,
                         DefaultSecurityManager securityManager,
                         GlobalAuthServiceConfig globalAuthServiceConfig,
                         ClusterConfigService clusterConfigService,
                         AuditEventSender auditEventSender) {
        this.userManagementService = userManagementService;
        this.accessTokenService = accessTokenService;
        this.roleService = roleService;
        this.sessionService = sessionService;
        this.paginatedUserService = paginatedUserService;
        this.sessionTerminationService = sessionTerminationService;
        this.securityManager = securityManager;
        this.searchQueryParser = new SearchQueryParser(UserOverviewDTO.FIELD_FULL_NAME, SEARCH_FIELD_MAPPING);
        this.globalAuthServiceConfig = globalAuthServiceConfig;
        this.clusterConfigService = clusterConfigService;
        this.auditEventSender = auditEventSender;
    }

    /**
     * @deprecated
     */
    @GET
    @Deprecated
    @Path("{username}")
    @Operation(summary = "Get user details", description = "The user's permissions are only included if a user asks for his " +
            "own account or for users with the necessary permissions to edit permissions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Returns the user", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "The user could not be found.")
    })
    public UserSummary get(@Parameter(name = "username", description = "The username to return information for.", required = true)
                           @PathParam("username") String username,
                           @Context UserContext userContext) {
        // If a user has permissions to edit another user's profile, it should be able to see it.
        // Reader users always have permissions to edit their own profile.
        if (!isPermitted(USERS_EDIT, username)) {
            throw new ForbiddenException("Not allowed to view user " + username);
        }

        final User user = userManagementService.load(username);
        if (user == null) {
            throw new NotFoundException("Couldn't find user " + username);
        }
        return returnSummary(userContext, user);
    }

    @GET
    @Path("id/{userId}")
    @Operation(summary = "Get user details by userId", description = "The user's permissions are only included if a user asks for his " +
            "own account or for users with the necessary permissions to edit permissions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Returns the user", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "The user could not be found.")
    })
    public UserSummary getbyId(@Parameter(name = "userId", description = "The userId to return information for.", required = true)
                               @PathParam("userId") String userId,
                               @Context UserContext userContext) {

        final User user = loadUserById(userId);
        final String username = user.getName();
        // If a user has permissions to edit another user's profile, it should be able to see it.
        // Reader users always have permissions to edit their own profile.
        if (!isPermitted(USERS_EDIT, username)) {
            throw new ForbiddenException("Not allowed to view userId " + userId);
        }
        return returnSummary(userContext, user);
    }

    private UserSummary returnSummary(UserContext userContext, User user) {
        final String requestingUser = userContext.getUser().getId();
        final boolean isSelf = requestingUser.equals(user.getId());
        final boolean canEditUserPermissions = isPermitted(USERS_PERMISSIONSEDIT, user.getName());

        return toUserResponse(user, isSelf || canEditUserPermissions, Optional.of(AllUserSessions.create(sessionService)));
    }

    @GET
    @Path("/basic/id/{userId}")
    @Operation(summary = "Get basic user data by userId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Returns user info", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "The user could not be found.")
    })
    public BasicUserResponse getBasicUserById(@Parameter(name = "userId", description = "The userId to return information for.", required = true)
                                              @PathParam("userId") String userId) {

        final User user = loadUserById(userId);
        if (!isPermitted(USERS_READ, user.getName())) {
            throw new ForbiddenException("Not allowed to view userId " + userId);
        }
        return BasicUserResponse.builder()
                .id(user.getId())
                .username(user.getName())
                .fullName(user.getFullName())
                .readOnly(user.isReadOnly())
                .isServiceAccount(user.isServiceAccount())
                .build();
    }

    /**
     * @deprecated Use the paginated call instead
     */
    @GET
    @Deprecated
    @Operation(summary = "List all users", description = "Permissions and session data included by default")
    public UserList listUsers(
            @Parameter(name = "include_permissions") @QueryParam("include_permissions") @DefaultValue("true") boolean includePermissions,
            @Parameter(name = "include_sessions") @QueryParam("include_sessions") @DefaultValue("true") boolean includeSessions,
            @Context SearchUser searchUser) {
        final Optional<AllUserSessions> optSessions = includeSessions ? Optional.of(AllUserSessions.create(sessionService)) : Optional.empty();
        return searchUser.isPermitted(RestPermissions.USERS_LIST) ?
                listUsersSelective(includePermissions, optSessions) :
                listForLoggedInUser(searchUser, includePermissions, optSessions);
    }

    private UserList listForLoggedInUser(final SearchUser searchUser, final boolean includePermissions, Optional<AllUserSessions> optSessions) {
        return UserList.create(List.of(toUserResponse(searchUser.getUser(), includePermissions, optSessions)));
    }

    private UserList listUsersSelective(final boolean includePermissions, final Optional<AllUserSessions> optSessions) {
        final List<User> users = userManagementService.loadAll();

        final List<UserSummary> resultUsers = Lists.newArrayListWithCapacity(users.size() + 1);
        userManagementService.getRootUser().ifPresent(adminUser -> {
                    if (isPermitted(USERS_READ, adminUser.getName())) {
                        resultUsers.add(toUserResponse(adminUser, includePermissions, optSessions));
                    }
                }
        );

        for (User user : users) {
            if (isPermitted(USERS_READ, user.getName())) {
                resultUsers.add(toUserResponse(user, includePermissions, optSessions));
            }
        }

        return UserList.create(resultUsers);
    }

    @GET
    @Timed
    @Path("/paginated")
    @Operation(summary = "Get paginated list of users")
    @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<UserOverviewDTO> getPage(@Parameter(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                      @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                      @Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                                      @Parameter(name = "sort",
                                                              description = "The field to sort the result on",
                                                              required = true,
                                                              schema = @Schema(allowableValues = {"username", "full_name", "email"}))
                                                      @DefaultValue(UserOverviewDTO.FIELD_FULL_NAME) @QueryParam("sort") String sort,
                                                      @Parameter(name = "order", description = "The sort direction",
                                                              schema = @Schema(allowableValues = {"asc", "desc"}))
                                                      @DefaultValue("asc") @QueryParam("order") SortOrder order) {

        SearchQuery searchQuery;
        final AllUserSessions sessions = AllUserSessions.create(sessionService);
        try {
            searchQuery = searchQueryParser.parse(query);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid argument in search query: " + e.getMessage());
        }

        final Predicate<String> userNamePermissionPredicate = username -> isPermitted(USERS_READ, username);
        final PaginatedList<UserOverviewDTO> result = paginatedUserService
                .findPaginated(userNamePermissionPredicate, searchQuery, page, perPage, sort, order);
        final Set<String> allRoleIds = result.stream().flatMap(userDTO -> {
            if (userDTO.roles() != null) {
                return userDTO.roles().stream();
            }
            return Stream.empty();
        }).collect(Collectors.toSet());

        Map<String, String> roleNameMap;
        try {
            roleNameMap = getRoleNameMap(allRoleIds);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new NotFoundException("Couldn't find roles: " + e.getMessage());
        }

        final Map<String, Object> adminContextMap = new HashMap<>();
        getAdminUserDTO(sessions).ifPresent(adminUserDTO -> adminContextMap.put("admin_user", adminUserDTO));

        final Optional<AuthServiceBackendDTO> activeAuthService = globalAuthServiceConfig.getActiveBackendConfig();

        List<UserOverviewDTO> users = result.stream().map(userDTO -> {
            UserOverviewDTO.Builder builder = userDTO.toBuilder()
                    .fillSession(sessions.forUser(userDTO));
            if (userDTO.roles() != null) {
                builder.roles(userDTO.roles().stream().map(roleNameMap::get).collect(Collectors.toSet()));
            }
            userDTO.authServiceId().ifPresent(
                    serviceId -> builder.authServiceEnabled(activeAuthService.isPresent() && serviceId.equals(activeAuthService.get().id()))
            );
            return builder.build();
        }).toList();

        final PaginatedList<UserOverviewDTO> userOverviewDTOS = new PaginatedList<>(users, result.pagination().total(),
                result.pagination().page(), result.pagination().perPage());
        return PaginatedResponse.create("users", userOverviewDTOS, query, adminContextMap);
    }

    @POST
    @RequiresPermissions(RestPermissions.USERS_CREATE)
    @Operation(summary = "Create a new user account.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid user details.")
    })
    @AuditEvent(type = AuditEventTypes.USER_CREATE)
    public Response create(@RequestBody(description = "Must contain username, full_name, email, password and a list of permissions.", required = true)
                               @Valid @NotNull CreateUserRequest cr,
                           @Context UserContext userContext) throws ValidationException {
        if (isUserNameInUse(cr.username())) {
            final String msg = "Cannot create user " + cr.username() + ". Username is already taken.";
            LOG.error(msg);
            throw new BadRequestException(msg);
        }
        if (rolesContainAdmin(cr.roles()) && cr.isServiceAccount()) {
            throw new BadRequestException("Cannot assign Admin role to service account");
        }
        validatePasswordComplexity(cr.password());

        // Create user.
        User user = userManagementService.create();
        user.setName(cr.username());
        user.setPassword(cr.password());
        user.setFirstLastFullNames(cr.firstName(), cr.lastName());
        user.setEmail(cr.email());
        user.setPermissions(cr.permissions());
        setUserRoles(cr.roles(), user, userContext);
        user.setServiceAccount(cr.isServiceAccount());

        if (cr.timezone() != null) {
            user.setTimeZone(cr.timezone());
        }

        final Long sessionTimeoutMs = cr.sessionTimeoutMs();
        if (sessionTimeoutMs != null) {
            user.setSessionTimeoutMs(sessionTimeoutMs);
        }

        final Startpage startpage = cr.startpage();
        if (startpage != null) {
            user.setStartpage(startpage);
        }

        final String id = userManagementService.create(user, getCurrentUser());
        LOG.debug("Saved user {} with id {}", user.getName(), id);

        final URI userUri = getUriBuilderToSelf().path(UsersResource.class)
                .path("{username}")
                .build(user.getName());

        return Response.created(userUri).build();
    }

    @GET
    @RequiresPermissions(RestPermissions.USERS_CREATE)
    @Path("/username_availability")
    @Operation(summary = "Check if a username is still available")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkUsernameAvailability(@Parameter(name = "username") @QueryParam("username") String username) {
        return Response.ok(new UsernameAvailabilityResponse(username, !isUserNameInUse(username))).build();
    }

    private boolean isUserNameInUse(String username) {
        return userManagementService.load(username) != null;
    }

    private void validatePasswordComplexity(String password) {
        PasswordComplexityConfig config = clusterConfigService.getOrDefault(PasswordComplexityConfig.class, PasswordComplexityConfig.DEFAULT);

        StringBuilder errorMessages = new StringBuilder();
        if (password == null || password.isBlank()) {
            errorMessages.append("Password cannot be empty.");
        } else {
            if (password.length() < config.minLength()) {
                errorMessages.append("Password must be at least ").append(config.minLength()).append(" characters long.\n");
            }
            if (config.requireUppercase() && password.chars().noneMatch(Character::isUpperCase)) {
                errorMessages.append("Password must contain at least one uppercase letter.\n");
            }
            if (config.requireLowercase() && password.chars().noneMatch(Character::isLowerCase)) {
                errorMessages.append("Password must contain at least one lowercase letter.\n");
            }
            if (config.requireNumbers() && password.chars().noneMatch(Character::isDigit)) {
                errorMessages.append("Password must contain at least one number.\n");
            }
            if (config.requireSpecialCharacters() && password.chars().noneMatch(SPECIAL_CHARACTERS_CODEPOINTS::contains)) {
                errorMessages.append("Password must contain at least one special character from: ").append(SPECIAL_CHARACTERS).append("\n");
            }
        }

        if (!errorMessages.isEmpty()) {
            String msg = errorMessages.toString();
            LOG.error(msg);
            throw new BadRequestException(msg);
        }
    }

    private void setUserRoles(@Nullable List<String> roles, User user, UserContext userContext) {
        if (roles == null) {
            return;
        }

        try {
            final Map<String, Role> nameMap = roleService.loadAllLowercaseNameMap();
            final Map<String, String> idToNameMap = nameMap.values().stream()
                    .collect(Collectors.toMap(Role::getId, r -> r.getName().toLowerCase(Locale.US)));

            final Set<String> normalizedRoles = roles.stream()
                    .map(r -> r.toLowerCase(Locale.US))
                    .collect(Collectors.toSet());

            final List<String> unknownRoles = normalizedRoles.stream()
                    .filter(r -> !nameMap.containsKey(r)).toList();

            if (!unknownRoles.isEmpty()) {
                throw new BadRequestException(
                        String.format(Locale.ENGLISH, "Invalid role names: %s", String.join(", ", unknownRoles))
                );
            }

            final Set<String> currentRoleNames = user.getRoleIds().stream()
                    .map(idToNameMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            final Set<String> changedRoles = Sets.symmetricDifference(normalizedRoles, currentRoleNames);

            for (String changedRole : changedRoles) {
                checkPermission(RestPermissions.ROLES_ASSIGN, nameMap.get(changedRole).getName());
            }

            final Set<String> roleIds = normalizedRoles.stream()
                    .map(nameMap::get)
                    .map(Role::getId)
                    .collect(Collectors.toSet());

            user.setRoleIds(roleIds);
            auditEventSender.success(AuditActor.user(userContext.getUser().getName()),
                    AuditEventTypes.USER_ROLES_UPDATE,
                    ImmutableMap.of("roles", String.join(",", normalizedRoles),
                            "userName", user.getName(),
                            "userId", user.getId()));

        } catch (org.graylog2.database.NotFoundException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @PUT
    @Path("{userId}")
    @Operation(summary = "Modify user details.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Attempted to modify a read only user account (e.g. built-in or LDAP users)."),
            @ApiResponse(responseCode = "400", description = "Missing or invalid user details.")
    })
    @AuditEvent(type = AuditEventTypes.USER_UPDATE)
    public void changeUser(@Parameter(name = "userId", description = "The ID of the user to modify.", required = true)
                           @PathParam("userId") String userId,
                           @RequestBody(description = "Updated user information.", required = true)
                           @Valid @NotNull ChangeUserRequest cr,
                           @Context UserContext userContext) throws ValidationException {

        final User user = loadUserById(userId);
        final String username = user.getName();
        checkPermission(USERS_EDIT, username);

        if (user.isReadOnly()) {
            throw new BadRequestException("Cannot modify readonly user " + username);
        }
        // We only allow setting a subset of the fields in ChangeUserRequest
        if (!user.isExternalUser()) {
            if (cr.email() != null) {
                user.setEmail(cr.email());
            }
            if (cr.firstName() != null && cr.lastName() != null) {
                user.setFirstLastFullNames(cr.firstName(), cr.lastName());
            }
        }
        final boolean permitted = isPermitted(USERS_PERMISSIONSEDIT, user.getName());
        if (permitted && cr.permissions() != null) {
            user.setPermissions(getEffectiveUserPermissions(user, cr.permissions()));
        }

        if (isPermitted(USERS_ROLESEDIT, user.getName())) {
            checkAdminRoleForServiceAccount(cr, user);
            setUserRoles(cr.roles(), user, userContext);
        }

        final String timezone = cr.timezone();
        if (timezone == null) {
            user.setTimeZone((String) null);
        } else {
            try {
                if (timezone.isEmpty()) {
                    user.setTimeZone((String) null);
                } else {
                    final DateTimeZone tz = DateTimeZone.forID(timezone);
                    user.setTimeZone(tz);
                }
            } catch (IllegalArgumentException e) {
                LOG.error("Invalid timezone '{}', ignoring it for user {}.", timezone, username);
            }
        }
        user.setStartpage(cr.startpage());

        if (isPermitted("*")) {
            final Long sessionTimeoutMs = cr.sessionTimeoutMs();
            if (Objects.nonNull(sessionTimeoutMs) && sessionTimeoutMs != 0 && (user.getSessionTimeoutMs() != sessionTimeoutMs)) {
                user.setSessionTimeoutMs(sessionTimeoutMs);
                terminateSessions(user);
            }
        }

        if (cr.isServiceAccount() != null) {
            user.setServiceAccount(cr.isServiceAccount());
        }

        userManagementService.update(user, cr);
    }

    private void terminateSessions(User user) {
        final List<Session> allSessions = sessionTerminationService.getActiveSessionsForUser(user);

        final Subject subject = getSubject();
        final Session currentSession = subject.getSession(false);
        final User currentUser = getCurrentUser();

        if (currentSession != null && currentUser != null && user.getId().equals(currentUser.getId())) {
            // Stop all sessions but handle the current session differently by issuing a proper logout
            allSessions.stream()
                    .filter(session -> !session.getId().equals(currentSession.getId()))
                    .forEach(Session::stop);
            securityManager.logout(subject);
        } else {
            allSessions.forEach(Session::stop);
        }
    }

    private boolean rolesContainAdmin(List<String> roles) {
        return roles != null && roles.stream().anyMatch(RoleServiceImpl.ADMIN_ROLENAME::equalsIgnoreCase);
    }

    private void checkAdminRoleForServiceAccount(ChangeUserRequest cr, User user) {
        if (user.isServiceAccount() && rolesContainAdmin(cr.roles())) {
            throw new BadRequestException("Cannot assign Admin role to service account");
        }
        if (cr.isServiceAccount() != null && cr.isServiceAccount()) {
            if (user.getRoleIds().contains(roleService.getAdminRoleObjectId())) {
                throw new BadRequestException("Cannot make Admin into service account");
            }
        }
    }

    @DELETE
    @Path("{username}")
    @Operation(summary = "Removes a user account.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Success"),
            @ApiResponse(responseCode = "400", description = "When attempting to remove a read only user (e.g. built-in or LDAP user).")
    })
    @AuditEvent(type = AuditEventTypes.USER_DELETE)
    public void deleteUser(@Parameter(name = "username", description = "The name of the user to delete.", required = true)
                           @PathParam("username") String username) {
        checkPermission(USERS_EDIT, username);

        if (userManagementService.delete(username) == 0) {
            throw new NotFoundException("Couldn't find user " + username);
        }
    }

    @DELETE
    @Path("id/{userId}")
    @Operation(summary = "Removes a user account.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Success"),
            @ApiResponse(responseCode = "400", description = "When attempting to remove a read only user (e.g. built-in or LDAP user).")
    })
    @AuditEvent(type = AuditEventTypes.USER_DELETE)
    public void deleteUserById(@Parameter(name = "userId", description = "The id of the user to delete.", required = true)
                               @PathParam("userId") String userId) {
        final User user = loadUserById(userId);
        checkPermission(USERS_EDIT, user.getName());

        if (userManagementService.deleteById(userId) == 0) {
            throw new NotFoundException("Couldn't find user " + userId);
        }
    }

    @PUT
    @Path("{username}/permissions")
    @RequiresPermissions(RestPermissions.USERS_PERMISSIONSEDIT)
    @Operation(summary = "Update a user's permission set.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid permission data.")
    })
    @AuditEvent(type = AuditEventTypes.USER_PERMISSIONS_UPDATE)
    public void editPermissions(@Parameter(name = "username", description = "The name of the user to modify.", required = true)
                                @PathParam("username") String username,
                                @RequestBody(description = "The list of permissions to assign to the user.", required = true)
                                @Valid @NotNull PermissionEditRequest permissionRequest) throws ValidationException {
        final User user = userManagementService.load(username);
        if (user == null) {
            throw new NotFoundException("Couldn't find user " + username);
        }

        user.setPermissions(getEffectiveUserPermissions(user, permissionRequest.permissions()));
        userManagementService.save(user);
    }

    @PUT
    @Path("{username}/preferences")
    @Operation(summary = "Update a user's preferences set.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid permission data.")
    })
    @AuditEvent(type = AuditEventTypes.USER_PREFERENCES_UPDATE)
    public void savePreferences(@Parameter(name = "username", description = "The name of the user to modify.", required = true)
                                @PathParam("username") String username,
                                @RequestBody(description = "The map of preferences to assign to the user.", required = true)
                                UpdateUserPreferences preferencesRequest) throws ValidationException {
        final User user = userManagementService.load(username);
        checkPermission(USERS_EDIT, username);

        if (user == null) {
            throw new NotFoundException("Couldn't find user " + username);
        }

        user.setPreferences(preferencesRequest.preferences());
        userManagementService.save(user);
    }

    @DELETE
    @Path("{username}/permissions")
    @RequiresPermissions(RestPermissions.USERS_PERMISSIONSEDIT)
    @Operation(summary = "Revoke all permissions for a user without deleting the account.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Success"),
            @ApiResponse(responseCode = "500", description = "When saving the user failed.")
    })
    @AuditEvent(type = AuditEventTypes.USER_PERMISSIONS_DELETE)
    public void deletePermissions(@Parameter(name = "username", description = "The name of the user to modify.", required = true)
                                  @PathParam("username") String username) throws ValidationException {
        final User user = userManagementService.load(username);
        if (user == null) {
            throw new NotFoundException("Couldn't find user " + username);
        }
        user.setPermissions(Collections.emptyList());
        userManagementService.save(user);
    }

    @PUT
    @Path("{userId}/password")
    @Operation(summary = "Update the password for a user.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "The password was successfully updated. Subsequent requests must be made with the new password."),
            @ApiResponse(responseCode = "400", description = "The new password is missing, or the old password is missing or incorrect."),
            @ApiResponse(responseCode = "403", description = "The requesting user has insufficient privileges to update the password for the given user."),
            @ApiResponse(responseCode = "404", description = "User does not exist.")
    })
    @AuditEvent(type = AuditEventTypes.USER_PASSWORD_UPDATE)
    public void changePassword(
            @Parameter(name = "userId", description = "The id of the user whose password to change.", required = true)
            @PathParam("userId") String userId,
            @RequestBody(description = "The old and new passwords.", required = true)
            @Valid ChangePasswordRequest cr) throws ValidationException {

        final User user = loadUserById(userId);
        final String username = user.getName();

        if (!getSubject().isPermitted(RestPermissions.USERS_PASSWORDCHANGE + ":" + username)) {
            throw new ForbiddenException("Not allowed to change password for user " + username);
        }
        if (user.isExternalUser()) {
            final String msg = "Cannot change password for external user.";
            LOG.error(msg);
            throw new ForbiddenException(msg);
        }

        boolean checkOldPassword = true;
        // users with the wildcard permission for password change do not have to supply the old password, unless they try to change their own password.
        // the rationale is to prevent accidental or malicious change of admin passwords (e.g. to prevent locking out legitimate admins)
        if (getSubject().isPermitted(RestPermissions.USERS_PASSWORDCHANGE + ":*")) {
            if (username.equals(getSubject().getPrincipal())) {
                LOG.debug("User {} is allowed to change the password of any user, but attempts to change own password. Must supply the old password.", getSubject().getPrincipal());
                checkOldPassword = true;
            } else {
                LOG.debug("User {} is allowed to change the password for any user, including {}, ignoring old password", getSubject().getPrincipal(), username);
                checkOldPassword = false;
            }
        }

        boolean changeAllowed = false;
        if (checkOldPassword) {
            if (userManagementService.isUserPassword(user, cr.oldPassword())) {
                changeAllowed = true;
            }
        } else {
            changeAllowed = true;
        }

        if (changeAllowed) {
            validatePasswordComplexity(cr.password());
            if (checkOldPassword) {
                userManagementService.changePassword(user, cr.oldPassword(), cr.password());
            } else {
                userManagementService.changePassword(user, cr.password());
            }
        } else {
            throw new BadRequestException("Old password is missing or incorrect.");
        }
    }

    @PUT
    @Path("{userId}/status/{newStatus}")
    @Consumes(MediaType.WILDCARD)
    @Operation(summary = "Update the account status for a user")
    @AuditEvent(type = AuditEventTypes.USER_UPDATE)
    public Response updateAccountStatus(
            @Parameter(name = "userId", description = "The id of the user whose status to change.", required = true)
            @PathParam("userId") @NotBlank String userId,
            @Parameter(name = "newStatus", description = "The account status to be set", required = true,
                      schema = @Schema(allowableValues = {"enabled", "disabled", "deleted"}))
            @PathParam("newStatus") @NotBlank String newStatusString,
            @Context UserContext userContext) throws ValidationException {

        if (userId.equalsIgnoreCase(userContext.getUserId())) {
            throw new BadRequestException("Users are not allowed to set their own status");
        }

        final User.AccountStatus newStatus = User.AccountStatus.valueOf(newStatusString.toUpperCase(Locale.US));
        final User user = loadUserById(userId);
        checkPermission(USERS_EDIT, user.getName());
        final User.AccountStatus oldStatus = user.getAccountStatus();

        if (oldStatus.equals(newStatus)) {
            return Response.notModified().build();
        }

        userManagementService.setUserStatus(user, newStatus);
        return Response.ok().build();
    }

    @GET
    @Path("{userId}/tokens")
    @Operation(summary = "Retrieves the list of access tokens for a user")
    public TokenList listTokens(@Parameter(name = "userId", required = true)
                                @PathParam("userId") String userId) {
        final User user = loadUserById(userId);
        final String username = user.getName();

        if (!isPermitted(USERS_TOKENLIST, username)) {
            throw new ForbiddenException("Not allowed to list tokens for user " + username);
        }

        final ImmutableList.Builder<TokenSummary> tokenList = ImmutableList.builder();
        for (AccessToken token : accessTokenService.loadAll(user.getName())) {
            tokenList.add(TokenSummary.create(token.getId(), token.getName(), token.getLastAccess(), token.getCreatedAt(), token.getExpiresAt()));
        }

        return TokenList.create(tokenList.build());
    }

    @POST
    @Path("{userId}/tokens/{name}")
    @Operation(summary = "Generates a new access token for a user")
    @AuditEvent(type = AuditEventTypes.USER_ACCESS_TOKEN_CREATE)
    public Token generateNewToken(
            @Parameter(name = "userId", required = true) @PathParam("userId") String userId,
            @Parameter(name = "name", description = "Descriptive name for this token (e.g. 'cronjob') ", required = true) @PathParam("name") String name,
            @RequestBody(description = "Can optionally contain the token's TTL.") GenerateTokenTTL body) {
        final User futureOwner = loadUserById(userId);

        if (!isPermitted(USERS_TOKENCREATE, futureOwner.getName())) {
            throw new ForbiddenException("You are not allowed to create a token for user " + futureOwner.getName() + ".");
        }

        if (body == null) {
            body = new GenerateTokenTTL(Optional.empty());
        }
        final AccessToken accessToken = accessTokenService.create(futureOwner.getName(), name, body.getTTL(() -> clusterConfigService.getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES).defaultTTLForNewTokens()));

        return Token.create(accessToken.getId(), accessToken.getName(), accessToken.getToken(), accessToken.getLastAccess());
    }

    @DELETE
    @Path("{userId}/tokens/{idOrToken}")
    @Operation(summary = "Removes a token for a user")
    @AuditEvent(type = AuditEventTypes.USER_ACCESS_TOKEN_DELETE)
    public void revokeToken(
            @Parameter(name = "userId", required = true) @PathParam("userId") String userId,
            @Parameter(name = "idOrToken", required = true) @PathParam("idOrToken") String idOrToken) {
        final User user = loadUserById(userId);
        final String username = user.getName();
        if (!isPermitted(USERS_TOKENREMOVE, username)) {
            throw new ForbiddenException("Not allowed to remove tokens for user " + username);
        }

        // The endpoint supports both, deletion by token ID and deletion by using the token value itself.
        // The latter should not be used anymore because the plain text token will be part of the URL and URLs
        // will most probably be logged. We keep the old behavior for backwards compatibility.
        // TODO: Remove support for old behavior in 4.0
        final AccessToken accessToken = Optional.ofNullable(accessTokenService.loadById(idOrToken))
                .orElse(accessTokenService.load(idOrToken));

        if (accessToken != null) {
            accessTokenService.destroy(accessToken);
        } else {
            throw new NotFoundException("Couldn't find access token for user " + username);
        }
    }

    private User loadUserById(String userId) {
        final User user = userManagementService.loadById(userId);
        if (user == null) {
            throw new NotFoundException("Couldn't find user with ID <" + userId + ">");
        }
        return user;
    }

    private UserSummary toUserResponse(User user, boolean includePermissions, Optional<AllUserSessions> optSessions) {
        final Set<String> roleIds = user.getRoleIds();
        Set<String> roleNames = Collections.emptySet();

        if (!roleIds.isEmpty()) {
            roleNames = userManagementService.getRoleNames(user);

            if (roleNames.isEmpty()) {
                LOG.error("Unable to load role names for role IDs {} for user {}", roleIds, user);
            }
        }

        boolean sessionActive = false;
        Date lastActivity = null;
        String clientAddress = null;
        if (optSessions.isPresent()) {
            final AllUserSessions sessions = optSessions.get();
            final Optional<SessionDTO> mongoDbSession = sessions.forUser(user);
            if (mongoDbSession.isPresent()) {
                final SessionDTO session = mongoDbSession.get();
                sessionActive = true;
                lastActivity = Date.from(session.lastAccessTime());
                clientAddress = session.host().orElse(null);
            }
        }
        List<WildcardPermission> wildcardPermissions;
        List<GRNPermission> grnPermissions;
        if (includePermissions) {
            wildcardPermissions = addUserIdPermissions(userManagementService.getWildcardPermissionsForUser(user), user);
            grnPermissions = userManagementService.getGRNPermissionsForUser(user);
        } else {
            wildcardPermissions = List.of();
            grnPermissions = List.of();
        }

        final Optional<AuthServiceBackendDTO> activeAuthService = globalAuthServiceConfig.getActiveBackendConfig();
        boolean authServiceEnabled =
                Objects.isNull(user.getAuthServiceId()) ||
                        (activeAuthService.isPresent() && user.getAuthServiceId().equals(activeAuthService.get().id())
                        );

        return UserSummary.create(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getFirstName().orElse(null),
                user.getLastName().orElse(null),
                user.getFullName(),
                wildcardPermissions,
                grnPermissions,
                user.getPreferences(),
                user.getTimeZone() == null ? null : user.getTimeZone().getID(),
                user.getSessionTimeoutMs(),
                user.isReadOnly(),
                user.isExternalUser(),
                user.getStartpage(),
                roleNames,
                sessionActive,
                lastActivity,
                clientAddress,
                user.getAccountStatus(),
                user.isServiceAccount(),
                authServiceEnabled
        );
    }

    // Filter the permissions granted by roles from the permissions list
    private List<String> getEffectiveUserPermissions(final User user, final List<String> permissions) {
        final List<String> effectivePermissions = Lists.newArrayList(permissions);
        effectivePermissions.removeAll(userManagementService.getUserPermissionsFromRoles(user));
        return effectivePermissions;
    }

    private Map<String, String> getRoleNameMap(Set<String> roleIds) throws org.graylog2.database.NotFoundException {
        final Map<String, Role> roleMap = roleService.findIdMap(roleIds);
        final Map<String, String> result = new HashMap<>(roleMap.size());
        roleMap.forEach((key, value) -> result.put(key, value.getName()));
        return result;
    }

    private Optional<UserOverviewDTO> getAdminUserDTO(AllUserSessions sessions) {
        return userManagementService
                .getRootUser()
                .filter(rootUser -> isPermitted(USERS_READ, rootUser.getName()))
                .map(rootUser -> {
                    final Set<String> adminRoles = userManagementService.getRoleNames(rootUser);
                    final Optional<SessionDTO> lastSession = sessions.forUser(rootUser);
                    return UserOverviewDTO.builder()
                            .username(rootUser.getName())
                            .fullName(rootUser.getFullName())
                            .email(rootUser.getEmail())
                            .externalUser(rootUser.isExternalUser())
                            .readOnly(rootUser.isReadOnly())
                            .id(rootUser.getId())
                            .fillSession(lastSession)
                            .roles(adminRoles)
                            .build();
                });
    }

    /**
     * This method duplicates username-based user permissions with userId variants
     * (users:<action>:<userId>) so the UI can work when only the userId is known.
     * Do NOT add these id-based variants for backend authorization; server-side
     * permission checks must continue to use the original username form.
     */
    private List<WildcardPermission> addUserIdPermissions(List<WildcardPermission> permissions, User user) {
        ImmutableSet.Builder<WildcardPermission> builder = ImmutableSet.builder();
        final Map<String, String> userIdCache = new HashMap<>();
        if (user != null) {
            userIdCache.put(user.getName(), user.getId());
        }
        for (WildcardPermission permission : permissions) {
            String[] parts = permission.toString().split(":");
            boolean hasUsername = parts.length == USERNAME_PERMISSION_PARTS_LENGTH && USER_PERMISSION_DOMAIN.equals(parts[0]);
            if (hasUsername) {
                String username = parts[parts.length-1];
                String userId = userIdCache.computeIfAbsent(username, name -> {
                    final User loadedUser = userManagementService.load(name);
                    return loadedUser != null ? loadedUser.getId() : null;
                });
                if (userId != null) {
                    parts[2] = userId;
                    builder.add(new CaseSensitiveWildcardPermission(String.join(":", parts)));
                }
            }

        }
        return builder.addAll(permissions).build().asList();
    }

    public record GenerateTokenTTL(@JsonProperty Optional<PeriodDuration> tokenTTL) {
        public PeriodDuration getTTL(Supplier<PeriodDuration> defaultSupplier) {
            return this.tokenTTL.orElseGet(defaultSupplier);
        }
    }

    private static class AllUserSessions {
        private final Map<String, Optional<SessionDTO>> sessions;

        public static AllUserSessions create(SessionService sessionService) {
            try (var sessionDTOStream = sessionService.streamAll()) {
                return new AllUserSessions(sessionDTOStream.toList());
            }
        }

        private AllUserSessions(Collection<SessionDTO> sessions) {
            this.sessions = getLastSessionForUser(sessions);
        }

        public Optional<SessionDTO> forUser(User user) {
            return sessions.getOrDefault(user.getId(), Optional.empty());
        }

        public Optional<SessionDTO> forUser(UserOverviewDTO user) {
            return sessions.getOrDefault(user.id(), Optional.empty());
        }

        // Among all active sessions, find the last recently used for each user
        private Map<String, Optional<SessionDTO>> getLastSessionForUser(Collection<SessionDTO> sessions) {
            return sessions.stream()
                    .filter(s -> s.userId().isPresent())
                    .collect(groupingBy(s -> s.userId().get(),
                            maxBy(Comparator.comparing(SessionDTO::lastAccessTime))));
        }
    }
}
