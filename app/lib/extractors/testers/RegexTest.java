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
package lib.extractors.testers;

import com.google.common.collect.Maps;

import lib.APIException;
import lib.Api;
import models.api.responses.RegexTestResponse;

import java.io.IOException;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class RegexTest {

    public static Map<String, Object> test(String regex, String string) throws IOException, APIException {
        String pRegex = Api.urlEncode(regex);
        String pString = Api.urlEncode(string);

        String part = "/tools/regex_tester?regex=" + pRegex + "&string=" + pString;

        RegexTestResponse r = Api.get(part, RegexTestResponse.class);

        Map<String, Object> match = Maps.newHashMap();
        match.put("start", r.match.start);
        match.put("end", r.match.end);
        match.put("match", r.match.match);

        Map<String, Object> result = Maps.newHashMap();
        result.put("string", r.string);
        result.put("regex", r.regex);
        result.put("finds", r.matched);

        if (r.matched) {
            result.put("match", match);
        }

        return result;
    }

}
