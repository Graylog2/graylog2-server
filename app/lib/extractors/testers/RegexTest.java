/*
 * Copyright 2012-2015 TORCH GmbH, 2015 Graylog, Inc.
 *
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
package lib.extractors.testers;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.responses.RegexTestResponse;

import java.io.IOException;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class RegexTest {

    private final ApiClient api;

    @Inject
    private RegexTest(ApiClient api) {
        this.api = api;
    }

    public Map<String, Object> test(String regex, String string) throws IOException, APIException {
        RegexTestResponse r = api.get(RegexTestResponse.class)
                .path("/tools/regex_tester")
                .queryParam("regex", regex)
                .queryParam("string", string)
                .execute();

        Map<String, Object> result = Maps.newHashMap();
        result.put("string", r.string);
        result.put("regex", r.regex);
        result.put("finds", r.matched);

        if (r.matched && r.match != null) {
            Map<String, Object> match = Maps.newHashMap();
            match.put("start", r.match.start);
            match.put("end", r.match.end);
            match.put("match", r.match.match);

            result.put("match", match);
        }

        return result;
    }

}
