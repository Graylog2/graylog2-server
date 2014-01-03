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
package models;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import lib.APIException;
import lib.ApiClient;
import models.api.responses.SourcesResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SourcesService {

    private final ApiClient api;

    @Inject
    private SourcesService(ApiClient api) {
        this.api = api;
    }

    public List<Source> all() throws APIException, IOException {
        List<Source> list = Lists.newArrayList();

        SourcesResponse response = api.get(SourcesResponse.class).path("/sources").execute();

        for (Map.Entry<String, Long> source : response.sources.entrySet()) {
            list.add(new Source(source.getKey(), source.getValue()));
        }

        return list;
    }

}
