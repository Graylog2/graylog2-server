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
import com.google.common.collect.Maps;
import lib.APIException;
import lib.Api;
import models.api.requests.InputLaunchRequest;
import models.api.responses.EmptyResponse;
import models.api.responses.InputTypeResponse;
import models.api.responses.InputTypesResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Input {

    public static Map<String, String> getTypes(Node node) throws IOException, APIException {
        return Api.get(node, "system/inputs/types", InputTypesResponse.class).types;
    }

    public static InputTypeResponse getTypeInformation(Node node, String type) throws IOException, APIException {
        return Api.get(node, "system/inputs/types/" + type, InputTypeResponse.class);
    }

    public static Map<String, InputTypeResponse> getAllTypeInformation(Node node) throws IOException, APIException {
        Map<String, InputTypeResponse> types = Maps.newHashMap();

        List<InputTypeResponse> bools = Lists.newArrayList();
        for (String type : getTypes(node).keySet()) {
            InputTypeResponse itr = getTypeInformation(node, type);
            types.put(itr.type, itr);
        }

        return types;
    }

    public static void launch(Node node, String title, String type, Map<String, Object> configuration, String userId) throws IOException, APIException {
        InputLaunchRequest request = new InputLaunchRequest();
        request.title = title;
        request.type = type;
        request.configuration = configuration;
        request.creatorUserId = userId;

        Api.post(node, "system/inputs", request, 202, EmptyResponse.class);
    }

}
