/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
 *
 */
package controllers;

import lib.APIException;
import com.google.gson.Gson;
import lib.NaturalDateTest;
import lib.extractors.testers.RegexTest;
import lib.extractors.testers.SplitAndIndexTest;
import lib.extractors.testers.SubstringTest;
import play.mvc.Result;

import java.io.IOException;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ToolsApiController extends AuthenticatedController {

    public Result regexTest(String regex, String string) {
        try {
            if (regex.isEmpty() || string.isEmpty()) {
                return badRequest();
            }

            return ok(new Gson().toJson(RegexTest.test(regex, string))).as("application/json");
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

            return ok(new Gson().toJson(SubstringTest.test(start, end, string))).as("application/json");
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        }
    }

    public Result splitAndIndexTest(String splitBy, int index, String string) {
        try {
            if (splitBy.isEmpty()|| index < 0 || string.isEmpty()) {
                return badRequest();
            }

            return ok(new Gson().toJson(SplitAndIndexTest.test(splitBy, index, string))).as("application/json");
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
            return ok(new Gson().toJson(NaturalDateTest.test(string))).as("application/json");
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
