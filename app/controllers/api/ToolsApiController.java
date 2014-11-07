/*
 * Copyright 2013 TORCH UG
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package controllers.api;

import com.google.inject.Inject;
import controllers.AuthenticatedController;
import lib.NaturalDateTest;
import lib.extractors.testers.RegexTest;
import lib.extractors.testers.SplitAndIndexTest;
import lib.extractors.testers.SubstringTest;
import org.graylog2.restclient.lib.APIException;
import play.libs.Json;
import play.mvc.Result;

import java.io.IOException;

public class ToolsApiController extends AuthenticatedController {

    private final RegexTest regexTest;
    private final SubstringTest substringTest;
    private final SplitAndIndexTest splitAndIndexTest;
    private final NaturalDateTest naturalDateTest;

    @Inject
    private ToolsApiController(RegexTest regexTest,
                               SubstringTest substringTest,
                               SplitAndIndexTest splitAndIndexTest,
                               NaturalDateTest naturalDateTest) {
        this.regexTest = regexTest;
        this.substringTest = substringTest;
        this.splitAndIndexTest = splitAndIndexTest;
        this.naturalDateTest = naturalDateTest;
    }

    public Result regexTest(String regex, String string) {
        try {
            if (regex.isEmpty() || string.isEmpty()) {
                return badRequest();
            }

            return ok(Json.toJson(regexTest.test(regex, string)));
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        }
    }

    public Result substringTest(int start, int end, String string) {
        try {
            if (start < 0 || end <= 0 || string.isEmpty()) {
                return badRequest();
            }

            return ok(Json.toJson(substringTest.test(start, end, string)));
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        }
    }

    public Result splitAndIndexTest(String splitBy, int index, String string) {
        try {
            if (splitBy.isEmpty() || index < 0 || string.isEmpty()) {
                return badRequest();
            }

            return ok(Json.toJson(splitAndIndexTest.test(splitBy, index, string)));
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
            return ok(Json.toJson(naturalDateTest.test(string)));
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
