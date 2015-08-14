/**
 * This file is part of Graylog.
 * <p/>
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */

package lib.extractors.testers;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.graylog2.rest.models.tools.requests.JsonTestRequest;
import org.graylog2.rest.models.tools.responses.JsonTesterResponse;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;

import java.io.IOException;
import java.util.Map;

public class JsonTest {
    private final ApiClient api;

    @Inject
    private JsonTest(ApiClient api) {
        this.api = api;
    }

    public Map<String, Object> test(JsonTestRequest request) throws IOException, APIException {
        final JsonTesterResponse r = api.post(JsonTesterResponse.class)
                .path("/tools/json_tester")
                .body(request)
                .execute();

        return ImmutableMap.<String, Object>builder()
                .put("string", r.string())
                .put("flatten", r.flatten())
                .put("list_separator", r.listSeparator())
                .put("key_separator", r.keySeparator())
                .put("kv_separator", r.kvSeparator())
                .put("matches", r.matches())
                .build();
    }
}
