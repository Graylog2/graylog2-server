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
package org.graylog2.rest.resources.system.indexer;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiresAuthentication
@Api(value = "Indexer/Indices/Templates", description = "Index Template Management")
@Path("/system/indexer/indices/templates")
@Produces(MediaType.APPLICATION_JSON)
public class IndexTemplatesResource extends RestResource {
    private final Indices indices;
    private final IndexSetRegistry indexSetRegistry;

    @Inject
    public IndexTemplatesResource(Indices indices, IndexSetRegistry indexSetRegistry) {
        this.indices = indices;
        this.indexSetRegistry = indexSetRegistry;
    }

    @GET
    @Path("{indexSetId}")
    @Timed
    @ApiOperation("Get index template for the given index set")
    public IndexTemplateResponse get(@ApiParam(name = "indexSetId") @PathParam("indexSetId") @NotBlank String indexSetId) {
        checkPermission(RestPermissions.INDEXSETS_READ, indexSetId);

        final IndexSet indexSet = indexSetRegistry.get(indexSetId)
                .orElseThrow(() -> new NotFoundException("Index set " + indexSetId + " not found"));

        return createResponse(indexSet);
    }

    @GET
    @Timed
    @ApiOperation("Get index templates for all index sets")
    public Set<IndexTemplateResponse> getAll() {
        checkPermission(RestPermissions.INDEXSETS_READ);

        return indexSetRegistry.getAll().stream()
                .filter(indexSet -> isPermitted(RestPermissions.INDEXSETS_READ, indexSet.getConfig().id()))
                .map(this::createResponse)
                .collect(Collectors.toSet());
    }

    @POST
    @Path("{indexSetId}/update")
    @Timed
    @ApiOperation("Updates the index template for the given index set in Elasticsearch")
    @AuditEvent(type = AuditEventTypes.ES_INDEX_TEMPLATE_UPDATE)
    public IndexTemplateResponse sync(@ApiParam(name = "indexSetId") @PathParam("indexSetId") @NotBlank String indexSetId) {
        checkPermission(RestPermissions.INDEXSETS_EDIT, indexSetId);

        final IndexSet indexSet = indexSetRegistry.get(indexSetId)
                .orElseThrow(() -> new NotFoundException("Index set " + indexSetId + " not found"));

        indices.ensureIndexTemplate(indexSet);

        return createResponse(indexSet);
    }

    @POST
    @Path("update")
    @Timed
    @ApiOperation("Updates the index templates for all index sets in Elasticsearch")
    @AuditEvent(type = AuditEventTypes.ES_INDEX_TEMPLATE_UPDATE)
    public Set<IndexTemplateResponse> syncAll() {
        return indexSetRegistry.getAll().stream()
                .filter(indexSet -> isPermitted(RestPermissions.INDEXSETS_EDIT, indexSet.getConfig().id()))
                .map(indexSet -> {
                    indices.ensureIndexTemplate(indexSet);
                    return createResponse(indexSet);
                })
                .collect(Collectors.toSet());
    }

    private IndexTemplateResponse createResponse(IndexSet indexSet) {
        return IndexTemplateResponse.create(indexSet.getConfig().indexTemplateName(), indices.getIndexTemplate(indexSet));
    }

    @AutoValue
    public static abstract class IndexTemplateResponse {
        @JsonProperty("name")
        public abstract String name();
        @JsonProperty("template")
        public abstract Map<String, Object> template();

        public static IndexTemplateResponse create(String name, Map<String, Object> template) {
            return new AutoValue_IndexTemplatesResource_IndexTemplateResponse(name, template);
        }
    }
}
