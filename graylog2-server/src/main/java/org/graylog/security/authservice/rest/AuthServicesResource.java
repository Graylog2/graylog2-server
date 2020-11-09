/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.security.authservice.rest;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.security.authservice.AuthServiceBackendDTO;
import org.graylog.security.authservice.DBAuthServiceBackendService;
import org.graylog.security.authservice.GlobalAuthServiceConfig;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.users.PaginatedUserService;
import org.graylog2.users.RoleService;
import org.graylog2.users.UserOverviewDTO;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/system/authentication/services")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "System/Authentication/Services", description = "Manage authentication services")
@RequiresAuthentication
public class AuthServicesResource extends RestResource {
    private static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put(UserOverviewDTO.FIELD_USERNAME, SearchQueryField.create(UserOverviewDTO.FIELD_USERNAME))
            .put(UserOverviewDTO.FIELD_FULL_NAME, SearchQueryField.create(UserOverviewDTO.FIELD_FULL_NAME))
            .put(UserOverviewDTO.FIELD_EMAIL, SearchQueryField.create(UserOverviewDTO.FIELD_EMAIL))
            .build();

    private final GlobalAuthServiceConfig authServiceConfig;
    private final PaginatedUserService userService;
    private final DBAuthServiceBackendService backendService;
    private final RoleService roleService;
    private final SearchQueryParser userSearchQueryParser;

    @Inject
    public AuthServicesResource(GlobalAuthServiceConfig authServiceConfig,
                                PaginatedUserService userService,
                                DBAuthServiceBackendService backendService,
                                RoleService roleService) {
        this.authServiceConfig = authServiceConfig;
        this.userService = userService;
        this.backendService = backendService;
        this.roleService = roleService;
        this.userSearchQueryParser = new SearchQueryParser(UserOverviewDTO.FIELD_FULL_NAME, SEARCH_FIELD_MAPPING);
    }

    @GET
    @Path("active-backend")
    @ApiOperation("Get active authentication service backend")
    @RequiresPermissions(RestPermissions.AUTH_SERVICE_GLOBAL_CONFIG_READ)
    public Response get() {
        final Optional<AuthServiceBackendDTO> activeConfig = getActiveBackendConfig();

        // We cannot use an ImmutableMap because the backend value can be null
        final Map<String, Object> response = new HashMap<>();
        response.put("backend", activeConfig.orElse(null));
        response.put("context", Collections.singletonMap("backends_total", backendService.countBackends()));

        return Response.ok(response).build();
    }

    @GET
    @Path("active-backend/users")
    @ApiOperation("Get paginated users for active authentication service backend")
    @RequiresPermissions({RestPermissions.AUTH_SERVICE_GLOBAL_CONFIG_READ, RestPermissions.USERS_LIST})
    public PaginatedResponse<UserOverviewDTO> getUsers(
            @ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
            @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
            @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query,
            @ApiParam(name = "sort", value = "The field to sort the result on", required = true, allowableValues = "username,full_name,email")
            @DefaultValue(UserOverviewDTO.FIELD_FULL_NAME) @QueryParam("sort") String sort,
            @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
            @DefaultValue("asc") @QueryParam("order") String order
    ) {
        final AuthServiceBackendDTO activeConfig = getActiveBackendConfig()
                .orElseThrow(() -> new NotFoundException("No active authentication service backend found"));

        final PaginatedList<UserOverviewDTO> userList = userService.findPaginatedByAuthServiceBackend(
                parseSearchQuery(query), page, perPage, sort, order, activeConfig.id());

        return PaginatedResponse.create(
                "users",
                userList,
                query,
                Collections.singletonMap("roles", createRoleContext(userList.delegate()))
        );
    }

    private Map<String, Object> createRoleContext(List<UserOverviewDTO> userList) {
        final Set<String> roleIds = userList.stream()
                .flatMap(user -> user.roles().stream())
                .collect(Collectors.toSet());
        try {
            return roleService.findIdMap(roleIds).values()
                    .stream()
                    .map(role -> Maps.immutableEntry(role.getId(), Collections.singletonMap("title", role.getName())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (org.graylog2.database.NotFoundException e) {
            throw new NotFoundException("Couldn't find roles: " + roleIds);
        }
    }

    private SearchQuery parseSearchQuery(String query) {
        try {
            return userSearchQueryParser.parse(query);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid argument in search query: " + e.getMessage());
        }
    }

    private Optional<AuthServiceBackendDTO> getActiveBackendConfig() {
        final Optional<AuthServiceBackendDTO> activeConfig = authServiceConfig.getActiveBackendConfig();

        activeConfig.ifPresent(backend -> checkPermission(RestPermissions.AUTH_SERVICE_BACKEND_READ, backend.id()));

        return activeConfig;
    }
}
