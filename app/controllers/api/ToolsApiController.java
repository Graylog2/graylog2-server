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

package controllers.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import controllers.AuthenticatedController;
import lib.NaturalDateTest;
import lib.extractors.testers.GrokTest;
import lib.extractors.testers.RegexTest;
import lib.extractors.testers.SplitAndIndexTest;
import lib.extractors.testers.SubstringTest;
import lib.json.Json;
import org.graylog2.rest.models.tools.requests.GrokTestRequest;
import org.graylog2.rest.models.tools.requests.RegexTestRequest;
import org.graylog2.rest.models.tools.requests.SplitAndIndexTestRequest;
import org.graylog2.rest.models.tools.requests.SubstringTestRequest;
import org.graylog2.restclient.lib.APIException;
import play.mvc.Result;

import java.io.IOException;

public class ToolsApiController extends AuthenticatedController {

    private final RegexTest regexTest;
    private final SubstringTest substringTest;
    private final SplitAndIndexTest splitAndIndexTest;
    private final NaturalDateTest naturalDateTest;
    private final GrokTest grokTest;

    @Inject
    private ToolsApiController(RegexTest regexTest,
                               SubstringTest substringTest,
                               SplitAndIndexTest splitAndIndexTest,
                               NaturalDateTest naturalDateTest,
                               GrokTest grokTest) {
        this.regexTest = regexTest;
        this.substringTest = substringTest;
        this.splitAndIndexTest = splitAndIndexTest;
        this.naturalDateTest = naturalDateTest;
        this.grokTest = grokTest;
    }

    public Result regexTest() {
        final JsonNode json = request().body().asJson();
        final RegexTestRequest request = Json.fromJson(json, RegexTestRequest.class);

        try {
            if (request.regex().isEmpty() || request.string().isEmpty()) {
                return badRequest();
            }

            return ok(Json.toJsonString(regexTest.test(request))).as("application/json");
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        }
    }

    public Result substringTest() {
        final JsonNode json = request().body().asJson();
        final SubstringTestRequest request = Json.fromJson(json, SubstringTestRequest.class);
        try {
            if (request.start() < 0 || request.end() <= 0 || request.string().isEmpty()) {
                return badRequest();
            }

            return ok(Json.toJsonString(substringTest.test(request))).as("application/json");
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        }
    }

    public Result splitAndIndexTest() {
        final JsonNode json = request().body().asJson();
        final SplitAndIndexTestRequest request = Json.fromJson(json, SplitAndIndexTestRequest.class);

        try {
            if (request.splitBy().isEmpty() || request.index() < 0 || request.string().isEmpty()) {
                return badRequest();
            }

            return ok(Json.toJsonString(splitAndIndexTest.test(request))).as("application/json");
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        }
    }

    public Result naturalDateTest(String string) {
        if (string.isEmpty()) {
            return badRequest();
        }

        try {
            return ok(Json.toJsonString(naturalDateTest.test(string))).as("application/json");
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            if (e.getHttpCode() == 422) {
                return status(422);
            }
            return internalServerError("api exception " + e);
        }
    }

    public Result grokTest() {
        final JsonNode json = request().body().asJson();
        final GrokTestRequest request = Json.fromJson(json, GrokTestRequest.class);

        if (request.pattern().isEmpty() || request.string().isEmpty()) {
            return badRequest();
        }

        try {
            return ok(Json.toJsonString(grokTest.test(request))).as("application/json");
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            if (e.getHttpCode() == 422) {
                return status(422);
            }
            return internalServerError("api exception " + e);
        }
    }
}
