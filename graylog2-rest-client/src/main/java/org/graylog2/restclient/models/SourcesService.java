/**
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
package org.graylog2.restclient.models;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.responses.SourcesResponse;
import org.graylog2.restroutes.generated.routes;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.graylog2.restclient.lib.Configuration.apiTimeout;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SourcesService {

    private final ApiClient api;

    @Inject
    private SourcesService(ApiClient api) {
        this.api = api;
    }

    public List<Source> all(int range) throws APIException, IOException {
        List<Source> list = Lists.newArrayList();

        SourcesResponse response = api.path(routes.SourcesResource().list(), SourcesResponse.class)
                .queryParam("range", range)
                .timeout(apiTimeout("sources_all", 20, TimeUnit.SECONDS))
                .execute();

        for (Map.Entry<String, Long> source : response.sources.entrySet()) {
            list.add(new Source(source.getKey(), source.getValue()));
        }

        return list;
    }

}
