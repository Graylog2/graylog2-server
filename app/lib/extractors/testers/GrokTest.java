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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.graylog2.rest.models.tools.requests.GrokTestRequest;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.responses.GrokTestResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class GrokTest {

    private final ApiClient api;

    @Inject
    private GrokTest(ApiClient api) {
        this.api = api;
    }

    public Map<String, Object> test(GrokTestRequest request) throws IOException, APIException {
        GrokTestResponse r = api.post(GrokTestResponse.class)
                .path("/tools/grok_tester")
                .body(request)
                .execute();

        ArrayList<Object> matches = Lists.newArrayList();
        final ImmutableList<GrokTestResponse.Match> matches1 = FluentIterable.from(r.matches).toSortedList(new Comparator<GrokTestResponse.Match>() {
            @Override
            public int compare(GrokTestResponse.Match o1, GrokTestResponse.Match o2) {
                return o1.name.compareTo(o2.name);
            }
        });
        for (GrokTestResponse.Match m : matches1) {
            final HashMap<Object, Object> map = Maps.newHashMap();
            map.put("name", m.name);
            map.put("value", m.match);
            matches.add(map);
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("string", r.string);
        result.put("pattern", r.pattern);
        result.put("finds", r.matched);

        if (r.matched) {
            result.put("matches", matches);
        }

        return result;
    }

}
