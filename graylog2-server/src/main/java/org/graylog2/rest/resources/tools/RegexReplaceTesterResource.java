/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.rest.resources.tools;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.ConfigurationException;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.inputs.extractors.RegexReplaceExtractor;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.rest.models.tools.requests.RegexReplaceTestRequest;
import org.graylog2.rest.models.tools.responses.RegexReplaceTesterResponse;
import org.graylog2.shared.rest.resources.RestResource;

import java.util.Collections;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

@RequiresAuthentication
@Path("/tools/regex_replace_tester")
public class RegexReplaceTesterResource extends RestResource {

    private final SafePattern safePattern;

    @Inject
    public RegexReplaceTesterResource(final SafePattern safePattern) {
        this.safePattern = safePattern;
    }

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
    @NoAuditEvent("only used to test regex replace extractor")
    public RegexReplaceTesterResponse testRegex(@Valid @NotNull RegexReplaceTestRequest r) {
        return testRegexReplaceExtractor(r.string(), r.regex(), r.replacement(), r.replaceAll());
    }

    private RegexReplaceTesterResponse testRegexReplaceExtractor(final String example, final String regex,
                                                          final String replacement, final boolean replaceAll) {
        final Map<String, Object> config = ImmutableMap.of(
                "regex", regex,
                "replacement", replacement,
                "replace_all", replaceAll
        );
        final RegexReplaceExtractor extractor;
        try {
            extractor = new RegexReplaceExtractor(
                    new MetricRegistry(), "test", "Test", 0L, Extractor.CursorStrategy.COPY,
                    "test", "test", config, "tester",
                    Collections.<Converter>emptyList(), Extractor.ConditionType.NONE, "");
        } catch (PatternSyntaxException e) {
            throw new BadRequestException("Invalid regular expression: " + e.getMessage(), e);
        } catch (Extractor.ReservedFieldException e) {
            throw new BadRequestException("Trying to overwrite a reserved message field", e);
        } catch (ConfigurationException e) {
            throw new BadRequestException("Invalid extractor configuration", e);
        }

        try {
            final Extractor.Result result = extractor.runExtractor(safePattern.timeLimitedInput(example));
            final RegexReplaceTesterResponse.Match match = result == null ? null :
                    RegexReplaceTesterResponse.Match.create(
                            String.valueOf(result.getValue()), result.getBeginIndex(), result.getEndIndex());
            return RegexReplaceTesterResponse.create(result != null, match, regex, replacement, replaceAll, example);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage(), e);
        } catch (TimeLimitedCharSequence.TimeoutException e) {
            throw new BadRequestException("Regular expression matching timed out — the pattern may be susceptible to catastrophic backtracking");
        } catch (RuntimeException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IndexOutOfBoundsException || cause instanceof IllegalArgumentException) {
                throw new BadRequestException("Invalid replacement expression: " + cause.getMessage(), e);
            }
            throw e;
        }
    }
}
