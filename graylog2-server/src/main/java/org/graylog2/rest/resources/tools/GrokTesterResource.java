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
import com.google.common.collect.Lists;
import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.Match;
import oi.thekraken.grok.api.exception.GrokException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.GrokPatternService;
import org.graylog2.rest.models.tools.requests.GrokTestRequest;
import org.graylog2.rest.resources.tools.responses.GrokTesterResponse;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiresAuthentication
@Path("/tools/grok_tester")
@Produces(MediaType.APPLICATION_JSON)
public class GrokTesterResource extends RestResource {

    private final GrokPatternService grokPatternService;

    @Inject
    public GrokTesterResource(GrokPatternService grokPatternService) {
        this.grokPatternService = grokPatternService;
    }

    @GET
    @Timed
    public GrokTesterResponse grokTest(@QueryParam("pattern") @NotEmpty String pattern,
                                       @QueryParam("string") @NotNull String string,
                                       @QueryParam("named_captures_only") @NotNull boolean namedCapturesOnly) throws GrokException {

        return doTestGrok(string, pattern, namedCapturesOnly);
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @NoAuditEvent("only used to test Grok patterns")
    public GrokTesterResponse testGrok(@Valid @NotNull GrokTestRequest grokTestRequest) throws GrokException {
        return doTestGrok(grokTestRequest.string(), grokTestRequest.pattern(), grokTestRequest.namedCapturesOnly());
    }

    private GrokTesterResponse doTestGrok(String string, String pattern, boolean namedCapturesOnly) throws GrokException {
        final Set<GrokPattern> grokPatterns = grokPatternService.loadAll();

        final Grok grok = new Grok();
        for (GrokPattern grokPattern : grokPatterns) {
            grok.addPattern(grokPattern.name(), grokPattern.pattern());
        }

        grok.compile(pattern, namedCapturesOnly);
        final Match match = grok.match(string);
        match.captures();
        final Map<String, Object> matches = match.toMap();

        final GrokTesterResponse response;
        if (matches.isEmpty()) {
            response = GrokTesterResponse.create(false, Collections.<GrokTesterResponse.Match>emptyList(), pattern, string);
        } else {
            final List<GrokTesterResponse.Match> responseMatches = Lists.newArrayList();
            for (final Map.Entry<String, Object> entry : matches.entrySet()) {
                final Object value = entry.getValue();
                if (value != null) {
                    responseMatches.add(GrokTesterResponse.Match.create(entry.getKey(), value.toString()));
                }
            }

            response = GrokTesterResponse.create(true, responseMatches, pattern, string);
        }
        return response;
    }
}
