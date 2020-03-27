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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.glassfish.jersey.server.ChunkedOutput;
import org.graylog.plugins.views.audit.ViewsAuditEventTypes;
import org.graylog.plugins.views.search.export.ChunkForwarder;
import org.graylog.plugins.views.search.export.MessagesExporter;
import org.graylog.plugins.views.search.export.MessagesRequest;
import org.graylog.plugins.views.search.export.SearchTypeOverrides;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.MoreMediaTypes;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.graylog.plugins.views.search.export.Defaults.createDefaultMessagesRequest;

@Api(value = "Search Messages")
@Path("/views/search/messages")
@RequiresAuthentication
public class MessagesResource extends RestResource implements PluginRestResource {
    private final MessagesExporter exporter;

    //allow mocking
    Supplier<ChunkedOutput<String>> chunkedOutputSupplier = () -> new ChunkedOutput<>(String.class);
    Consumer<Runnable> asyncRunner = this::runAsync;

    private void runAsync(Runnable runnable) {
        Executor e = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("chunked-messages-request").build());
        e.execute(runnable);
    }

    @Inject
    public MessagesResource(MessagesExporter exporter) {
        this.exporter = exporter;
    }

    @POST
    @Produces(MoreMediaTypes.TEXT_CSV)
    @AuditEvent(type = ViewsAuditEventTypes.MESSAGES_EXPORT)
    public ChunkedOutput<String> retrieve(@ApiParam MessagesRequest request) {
        ChunkedOutput<String> output = chunkedOutputSupplier.get();

        ChunkForwarder<String> fwd = ChunkForwarder.create(chunk -> writeTo(output, chunk), () -> close(output));

        final MessagesRequest req = request != null ? request : createDefaultMessagesRequest();

        asyncRunner.accept(() -> exporter.export(req, fwd));

        return output;
    }

    @POST
    @Path("{searchId}/{searchTypeId}")
    @Produces(MoreMediaTypes.TEXT_CSV)
    @AuditEvent(type = ViewsAuditEventTypes.MESSAGES_EXPORT)
    public Response retrieveForSearchType(
            @ApiParam @PathParam("searchId") String searchId,
            @ApiParam @PathParam("searchTypeId") String searchTypeId,
            @ApiParam SearchTypeOverrides overrides) {
        exporter.export(searchId, searchTypeId, overrides);
        return okResultFrom();
    }

    private Response okResultFrom() {
        return Response
                .ok()
                //.header("Content-Disposition", "attachment; filename=" + result.filename())
                .build();
    }

    private void close(ChunkedOutput<String> output) {
        try {
            output.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to close ChunkedOutput", e);
        }
    }

    private void writeTo(ChunkedOutput<String> output, String chunk) {
        try {
            output.write(chunk);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to ChunkedOutput", e);
        }
    }
}
