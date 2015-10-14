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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.ConfigurationException;
import org.graylog2.inputs.extractors.RegexReplaceExtractor;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.rest.models.tools.requests.RegexReplaceTestRequest;
import org.graylog2.rest.models.tools.responses.RegexReplaceTesterResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.Map;

@RequiresAuthentication
@Path("/tools/regex_replace_tester")
public class RegexReplaceTesterResource extends RestResource {
    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public RegexReplaceTesterResponse regexTester(@QueryParam("regex") @NotEmpty String regex,
                                                  @QueryParam("replacement") @NotNull String replacement,
                                                  @QueryParam("replace_all") @DefaultValue("false") boolean replaceAll,
                                                  @QueryParam("string") @NotNull String string) {
        return testRegexReplaceExtractor(string, regex, replacement, replaceAll);
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public RegexReplaceTesterResponse testRegex(@Valid @NotNull RegexReplaceTestRequest r) {
        return testRegexReplaceExtractor(r.string(), r.regex(), r.replacement(), r.replaceAll());
    }

    private RegexReplaceTesterResponse testRegexReplaceExtractor(String example, String regex, String replacement, boolean replaceAll) {
        final Map<String, Object> config = ImmutableMap.<String, Object>of(
                "regex", regex,
                "replacement", replacement,
                "replace_all", replaceAll
        );
        final RegexReplaceExtractor extractor;
        try {
            extractor = new RegexReplaceExtractor(
                    new MetricRegistry(), "test", "Test", 0L, Extractor.CursorStrategy.COPY, "test", "test",
                    config, getCurrentUser().getName(), Collections.<Converter>emptyList(), Extractor.ConditionType.NONE, ""
            );
        } catch (Extractor.ReservedFieldException e) {
            throw new BadRequestException("Trying to overwrite a reserved message field", e);
        } catch (ConfigurationException e) {
            throw new BadRequestException("Invalid extractor configuration", e);
        }

        final Extractor.Result result = extractor.runExtractor(example);
        final RegexReplaceTesterResponse.Match match = result == null ? null :
                RegexReplaceTesterResponse.Match.create(String.valueOf(result.getValue()), result.getBeginIndex(), result.getEndIndex());
        return RegexReplaceTesterResponse.create(result != null, match, regex, replacement, replaceAll, example);
    }
}
