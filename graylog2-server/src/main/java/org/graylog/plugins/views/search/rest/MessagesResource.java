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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.glassfish.jersey.server.ChunkedOutput;
import org.graylog.plugins.views.audit.ViewsAuditEventTypes;
import org.graylog.plugins.views.search.export.ChunkedResult;
import org.graylog.plugins.views.search.export.MessagesRequest;
import org.graylog.plugins.views.search.export.MessagesResult;
import org.graylog.plugins.views.search.export.SearchTypeExporter;
import org.graylog.plugins.views.search.export.SearchTypeOverrides;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.MoreMediaTypes;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Api(value = "Search Messages")
@Path("/views/search/messages")
@RequiresAuthentication
public class MessagesResource extends RestResource implements PluginRestResource {
    private final SearchTypeExporter exporter;

    @Inject
    public MessagesResource(SearchTypeExporter exporter) {
        this.exporter = exporter;
    }

    @POST
    @Produces(MoreMediaTypes.TEXT_CSV)
    @AuditEvent(type = ViewsAuditEventTypes.MESSAGES_EXPORT)
    public Response retrieve(@ApiParam @NotNull MessagesRequest request) {
        MessagesResult result = exporter.export(request);
        return okResultFrom(result);
    }

    @POST
    @Path("{search-id}/{search-type-id}")
    @Produces(MoreMediaTypes.TEXT_CSV)
    @AuditEvent(type = ViewsAuditEventTypes.MESSAGES_EXPORT)
    public Response retrieveForSearchType(
            @ApiParam @PathParam("search-id") String searchId,
            @ApiParam @PathParam("search-type-id") String searchTypeId,
            @ApiParam SearchTypeOverrides overrides) {
        MessagesResult result = exporter.export(searchId, searchTypeId, overrides);
        return okResultFrom(result);
    }

    private Response okResultFrom(MessagesResult result) {
        ChunkedOutput<ChunkedResult> chunkedOutput = chunkedOutputFrom(result);
        return Response
                .ok(chunkedOutput)
                .header("Content-Disposition", "attachment; filename=" + result.filename())
                .build();
    }

    private ChunkedOutput<ChunkedResult> chunkedOutputFrom(MessagesResult result) {
        ChunkedOutput<ChunkedResult> output = new ChunkedOutput<>(ChunkedResult.class);
        try {
            output.write(result.messages());
            output.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create ChunkedOutput for " + result, e);
        }
        return output;
    }
}
