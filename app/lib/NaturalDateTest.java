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
package lib;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.responses.NaturalDateTestResponse;

import java.io.IOException;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class NaturalDateTest {

    private final ApiClient api;

    @Inject
    private NaturalDateTest(ApiClient api) {
        this.api = api;
    }

    public Map<String, String> test(String string) throws APIException, IOException {
        NaturalDateTestResponse r = api.get(NaturalDateTestResponse.class)
                .path("/tools/natural_date_tester")
                .queryParam("string", string)
                .execute();

        Map<String, String> result = Maps.newHashMap();
        result.put("from", r.from);
        result.put("to", r.to);

        return result;
    }

}
