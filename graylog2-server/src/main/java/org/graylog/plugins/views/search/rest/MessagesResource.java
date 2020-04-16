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
package org.graylog.plugins.views.search.rest;

import com.google.common.collect.ImmutableSet;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.glassfish.jersey.server.ChunkedOutput;
import org.graylog.plugins.views.audit.ViewsAuditEventTypes;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchDomain;
import org.graylog.plugins.views.search.SearchExecutionGuard;
import org.graylog.plugins.views.search.export.ChunkedRunner;
import org.graylog.plugins.views.search.export.MessagesExporter;
import org.graylog.plugins.views.search.export.MessagesRequest;
import org.graylog.plugins.views.search.export.ResultFormat;
import org.graylog.plugins.views.search.export.SimpleMessageChunk;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.MoreMediaTypes;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.function.Consumer;
import java.util.function.Function;

@Api(value = "Search Messages")
@Path("/views/search/messages")
@RequiresAuthentication
public class MessagesResource extends RestResource implements PluginRestResource {

    private final MessagesExporter exporter;
    private final SearchDomain searchDomain;
    private final SearchExecutionGuard executionGuard;
    private final PermittedStreams permittedStreams;

    //allow mocking
    Function<Consumer<Consumer<SimpleMessageChunk>>, ChunkedOutput<SimpleMessageChunk>> asyncRunner = ChunkedRunner::runAsyncc;

    @Inject
    public MessagesResource(MessagesExporter exporter, SearchDomain searchDomain, SearchExecutionGuard executionGuard, PermittedStreams permittedStreams) {
        this.exporter = exporter;
        this.searchDomain = searchDomain;
        this.executionGuard = executionGuard;
        this.permittedStreams = permittedStreams;
    }

    @POST
    @Produces(MoreMediaTypes.TEXT_CSV)
    @AuditEvent(type = ViewsAuditEventTypes.MESSAGES_EXPORT)
    public ChunkedOutput<SimpleMessageChunk> retrieve(@ApiParam @Valid MessagesRequest request) {
        final MessagesRequest req = fillInIfNecessary(request);

        executionGuard.checkUserIsPermittedToSeeStreams(req.streams(), this::hasStreamReadPermission);

        return asyncRunner.apply(chunkConsumer -> exporter.export(req, chunkConsumer));
    }

    private MessagesRequest fillInIfNecessary(MessagesRequest requestFromClient) {
        MessagesRequest request = requestFromClient != null ? requestFromClient : MessagesRequest.withDefaults();

        if (request.streams().isEmpty()) {
            request = request.toBuilder().streams(loadAllAllowedStreamsForUser()).build();
        }
        return request;
    }

    @POST
    @Path("{searchId}")
    @Produces(MoreMediaTypes.TEXT_CSV)
    @AuditEvent(type = ViewsAuditEventTypes.MESSAGES_EXPORT)
    public ChunkedOutput<SimpleMessageChunk> retrieveForSearch(
            @ApiParam @PathParam("searchId") String searchId,
            @ApiParam @Valid ResultFormat formatFromClient) {
        Search search = loadSearch(searchId);

        ResultFormat format = emptyIfNull(formatFromClient);

        return asyncRunner.apply(chunkConsumer -> exporter.export(search, format, chunkConsumer));
    }

    @POST
    @Path("{searchId}/{searchTypeId}")
    @Produces(MoreMediaTypes.TEXT_CSV)
    @AuditEvent(type = ViewsAuditEventTypes.MESSAGES_EXPORT)
    public ChunkedOutput<SimpleMessageChunk> retrieveForSearchType(
            @ApiParam @PathParam("searchId") String searchId,
            @ApiParam @PathParam("searchTypeId") String searchTypeId,
            @ApiParam @Valid ResultFormat formatFromClient) {
        Search search = loadSearch(searchId);

        ResultFormat format = emptyIfNull(formatFromClient);

        return asyncRunner.apply(chunkConsumer -> exporter.export(search, searchTypeId, format, chunkConsumer));
    }

    private ResultFormat emptyIfNull(ResultFormat format) {
        return format == null ? ResultFormat.empty() : format;
    }

    private Search loadSearch(String searchId) {
        Search search = searchDomain.getForUser(searchId, getCurrentUser(), this::hasViewReadPermission)
                .orElseThrow(() -> new NotFoundException("Search with id " + searchId + " does not exist"));

        search = search.addStreamsToQueriesWithoutStreams(this::loadAllAllowedStreamsForUser);

        executionGuard.check(search, this::hasStreamReadPermission);

        return search;
    }

    private boolean hasViewReadPermission(ViewDTO view) {
        return isPermitted(ViewsRestPermissions.VIEW_READ, view.id());
    }

    private ImmutableSet<String> loadAllAllowedStreamsForUser() {
        return permittedStreams.load(this::hasStreamReadPermission);
    }

    private boolean hasStreamReadPermission(String streamId) {
        return isPermitted(RestPermissions.STREAMS_READ, streamId);
    }
}
