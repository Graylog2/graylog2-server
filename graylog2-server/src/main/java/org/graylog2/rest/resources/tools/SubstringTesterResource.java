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
import org.graylog2.plugin.Tools;
import org.graylog2.rest.models.tools.requests.SubstringTestRequest;
import org.graylog2.rest.models.tools.responses.SubstringTesterResponse;
import org.graylog2.shared.rest.resources.RestResource;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@RequiresAuthentication
@Path("/tools/substring_tester")
public class SubstringTesterResource extends RestResource {
    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public SubstringTesterResponse substringTester(@QueryParam("begin_index") @Min(0) int beginIndex,
                                                   @QueryParam("end_index") @Min(1) int endIndex,
                                                   @QueryParam("string") @NotNull String string) {
        return doSubstringTest(string, beginIndex, endIndex);
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @NoAuditEvent("only used for testing substring extractor")
    public SubstringTesterResponse testSubstring(@Valid @NotNull SubstringTestRequest substringTestRequest) {
        return doSubstringTest(substringTestRequest.string(), substringTestRequest.start(), substringTestRequest.end());
    }

    private SubstringTesterResponse doSubstringTest(String string, int beginIndex, int endIndex) {
        final String cut = Tools.safeSubstring(string, beginIndex, endIndex);

        return SubstringTesterResponse.create(cut != null, cut, beginIndex, endIndex);
    }
}
