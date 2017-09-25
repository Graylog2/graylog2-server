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
import org.graylog2.rest.models.tools.requests.RegexTestRequest;
import org.graylog2.rest.models.tools.responses.RegexTesterResponse;
import org.graylog2.shared.rest.resources.RestResource;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@RequiresAuthentication
@Path("/tools/regex_tester")
public class RegexTesterResource extends RestResource {
    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public RegexTesterResponse regexTester(@QueryParam("regex") @NotEmpty String regex,
                                           @QueryParam("string") @NotNull String string) {
        return doTestRegex(string, regex);
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @NoAuditEvent("only used to test regex values")
    public RegexTesterResponse testRegex(@Valid @NotNull RegexTestRequest regexTestRequest) {
        return doTestRegex(regexTestRequest.string(), regexTestRequest.regex());
    }

    private RegexTesterResponse doTestRegex(String example, String regex) {
        final Pattern pattern;
        try {
            pattern = Pattern.compile(regex, Pattern.DOTALL);
        } catch (PatternSyntaxException e) {
            throw new BadRequestException("Invalid regular expression: " + e.getMessage(), e);
        }

        final Matcher matcher = pattern.matcher(example);
        boolean matched = matcher.find();

        // Get the first matched group.
        final RegexTesterResponse.Match match;
        if (matched && matcher.groupCount() > 0) {
            match = RegexTesterResponse.Match.create(matcher.group(1), matcher.start(1), matcher.end(1));
        } else {
            match = null;
        }

        return RegexTesterResponse.create(matched, match, regex, example);
    }
}
