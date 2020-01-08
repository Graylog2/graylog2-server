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
package org.graylog.plugins.views.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Uninterruptibles;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog.plugins.views.search.errors.PermissionException;
import org.graylog.plugins.views.search.rest.PermittedStreams;
import org.graylog2.plugin.database.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

public class SearchDomain {
    private static final Logger LOG = LoggerFactory.getLogger(SearchDomain.class);

    private final SearchDbService dbService;
    private final QueryEngine queryEngine;
    private final SearchJobService searchJobService;
    private final ViewPermissions viewPermissions;
    private final PermittedStreams permittedStreams;
    private final SearchExecutionGuard executionGuard;
    private final ObjectMapper objectMapper;

    @Inject
    public SearchDomain(SearchDbService dbService, QueryEngine queryEngine, SearchJobService searchJobService, ViewPermissions viewPermissions, PermittedStreams permittedStreams, SearchExecutionGuard executionGuard, ObjectMapper objectMapper) {
        this.dbService = dbService;
        this.queryEngine = queryEngine;
        this.searchJobService = searchJobService;
        this.viewPermissions = viewPermissions;
        this.permittedStreams = permittedStreams;
        this.executionGuard = executionGuard;
        this.objectMapper = objectMapper;
    }

    //left in here to not break dependencies in enterprise plugin.
    @Deprecated()
    public Optional<Search> getForUser(String id, User user, Predicate<String> viewReadPermission) {
        ViewsUser viewsUser = ViewsUser.fromDbUser(user, streamId -> false, viewReadPermission, genericPermission -> false);

        final Optional<Search> search = dbService.get(id);

        search.ifPresent(s -> checkPermission(viewsUser, s));

        return search;
    }

    public Search find(String id, ViewsUser user) {
        final Search search = dbService.get(id)
                .orElseThrow(() -> new EntityNotFoundException(id, Search.class));

        checkPermission(user, search);

        return search;
    }

    public SearchJob executeAsync(String id, Map<String, Object> executionState, ViewsUser user) {

        Search search = find(id, user);

        search = search.addStreamsToQueriesWithoutStreams(() -> loadAllAllowedStreamsForUser(user::hasStreamReadPermission));

        guard(search, user::hasStreamReadPermission);

        search = search.applyExecutionState(objectMapper, firstNonNull(executionState, Collections.emptyMap()));

        return execute(search, user);
    }

    private ImmutableSet<String> loadAllAllowedStreamsForUser(Predicate<String> streamReadPermission) {
        return permittedStreams.load(streamReadPermission);
    }

    private void guard(Search search, Predicate<String> streamReadPermission) {
        this.executionGuard.check(search, streamReadPermission);
    }

    public SearchJob executeSync(Search search, ViewsUser user, long timeout) {

        search = search.addStreamsToQueriesWithoutStreams(() -> loadAllAllowedStreamsForUser(user::hasStreamReadPermission));

        guard(search, user::hasStreamReadPermission);

        final SearchJob runningSearchJob = execute(search, user);

        forceCompletion(runningSearchJob, timeout);

        return runningSearchJob;
    }

    protected void forceCompletion(SearchJob runningSearchJob, long timeout) {
        try {
            //noinspection UnstableApiUsage
            Uninterruptibles.getUninterruptibly(runningSearchJob.getResultFuture(), timeout, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            LOG.error("Error executing search job <{}>", runningSearchJob.getId(), e);
            throw new InternalServerErrorException("Error executing search job: " + e.getMessage());
        } catch (TimeoutException e) {
            throw new InternalServerErrorException("Timeout while executing search job");
        } catch (Exception e) {
            LOG.error("Other error", e);
            throw e;
        }
    }

    private SearchJob execute(Search search, ViewsUser user) {
        final SearchJob searchJob = searchJobService.create(search, user.getName());

        return queryEngine.execute(searchJob);
    }

    private void checkPermission(ViewsUser user, Search s) {
        if (!hasReadPermissionFor(user, s))
            throw new PermissionException("User " + user + " does not have permission to load search " + s.id());
    }

    public List<Search> getAllForUser(ViewsUser user) {
        return dbService.streamAll()
                .filter(s -> hasReadPermissionFor(user, s))
                .collect(Collectors.toList());
    }

    private boolean hasReadPermissionFor(ViewsUser user, Search search) {
        return user.isOwnerOf(search) || hasPermissionFromViews(search, user);
    }

    private boolean hasPermissionFromViews(Search search, ViewsUser user) {
        return viewPermissions.isSearchPermitted(search.id(), user.getDbUser(), user::hasViewReadPermission);
    }

    public Search create(Search search, ViewsUser user) {
        makeSureUserMayOverwriteExistingSearchIfOneExists(search, user);

        guard(search, user::hasStreamReadPermission);

        Search searchWithOwner = search.toBuilder().owner(user.getName()).build();

        final Search saved = dbService.save(searchWithOwner);

        if (saved == null || saved.id() == null) {
            throw new RuntimeException("Failed to save search");
        }
        return saved;
    }

    private void makeSureUserMayOverwriteExistingSearchIfOneExists(Search search, ViewsUser user) {
        final Optional<Search> previous = dbService.get(search.id());

        if (!previous.isPresent())
            return;

        if (!mayOverwrite(user, previous.get())) {
            throw new ForbiddenException("Unable to update search with id <" + search.id() + ">, already exists and user is not permitted to overwrite it.");
        }
    }

    private boolean mayOverwrite(ViewsUser user, Search previous) {
        return user.isAdmin() || user.isOwnerOf(previous);
    }
}
