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
package controllers;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.graylog2.restclient.lib.ApiClient;
import play.mvc.Controller;

import java.util.Map;

public class BaseController extends Controller {
    private ApiClient api;

    public ApiClient api() {
        return api;
    }

    @Inject
    public void setApi(ApiClient api) {
        this.api = api;
    }

    public Map<String, String> flattenFormUrlEncoded(Map<String, String[]> form) {
        Map<String, String> result = Maps.newHashMap();

        for (Map.Entry<String, String[]> e : form.entrySet()) {
            if (e.getValue()[0] != null) {
                result.put(e.getKey(), e.getValue()[0]);
            }
        }

        return result;
    }

}
