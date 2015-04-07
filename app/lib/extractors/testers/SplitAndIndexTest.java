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

package lib.extractors.testers;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.graylog2.rest.models.tools.requests.SplitAndIndexTestRequest;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.responses.SplitAndIndexTestResponse;

import java.io.IOException;
import java.util.Map;

public class SplitAndIndexTest {

    private final ApiClient api;

    @Inject
    private SplitAndIndexTest(ApiClient api) {
        this.api = api;
    }

    public Map<String, Object> test(SplitAndIndexTestRequest request) throws IOException, APIException {
        SplitAndIndexTestResponse r = api.post(SplitAndIndexTestResponse.class)
                .path("/tools/split_and_index_tester")
                .body(request)
                .execute();

        Map<String, Object> match = Maps.newHashMap();
        match.put("start", r.beginIndex);
        match.put("end", r.endIndex);

        Map<String, Object> result = Maps.newHashMap();
        result.put("successful", r.successful);
        result.put("cut", r.cut);
        result.put("match", match);

        return result;
    }

}
