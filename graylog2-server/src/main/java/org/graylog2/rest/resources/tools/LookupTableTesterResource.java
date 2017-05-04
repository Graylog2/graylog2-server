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
package org.graylog2.rest.resources.tools;

import com.codahale.metrics.annotation.Timed;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.plugin.lookup.LookupResult;
import org.graylog2.rest.models.tools.requests.LookupTableTestRequest;
import org.graylog2.rest.resources.tools.responses.LookupTableTesterResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.hibernate.validator.constraints.NotEmpty;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@RequiresAuthentication
@Path("/tools/lookup_table_tester")
@Produces(MediaType.APPLICATION_JSON)
public class LookupTableTesterResource extends RestResource {

    private final LookupTableService lookupTableService;

    @Inject
    public LookupTableTesterResource(final LookupTableService lookupTableService) {
        this.lookupTableService = lookupTableService;
    }

    @GET
    @Timed
    public LookupTableTesterResponse grokTest(@QueryParam("lookup_table_name") @NotEmpty String lookupTableName,
                                              @QueryParam("string") @NotEmpty String string) {
        return doTestLookupTable(string, lookupTableName);
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @NoAuditEvent("only used to test lookup tables")
    public LookupTableTesterResponse testLookupTable(@Valid @NotNull LookupTableTestRequest lookupTableTestRequest) {
        return doTestLookupTable(lookupTableTestRequest.string(), lookupTableTestRequest.lookupTableName());
    }

    private LookupTableTesterResponse doTestLookupTable(String string, String lookupTableName) {
        if (!lookupTableService.hasTable(lookupTableName)) {
            return LookupTableTesterResponse.error("Lookup table <" + lookupTableName + "> doesn't exist");
        }

        final LookupTableService.Function table = lookupTableService.newBuilder().lookupTable(lookupTableName).build();
        final LookupResult result = table.lookup(string.trim());

        if (result == null) {
            return LookupTableTesterResponse.emptyResult(string);
        }

        return LookupTableTesterResponse.result(string, result);
    }
}
