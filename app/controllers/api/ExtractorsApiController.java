/**
 * Copyright 2014 Lennart Koopmann <lennart@torch.sh>
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
package controllers.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import controllers.AuthenticatedController;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.ExtractorService;
import org.graylog2.restclient.models.InputService;
import org.graylog2.restclient.models.NodeService;
import play.Logger;
import play.mvc.BodyParser;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Iterator;
import java.util.SortedMap;

public class ExtractorsApiController extends AuthenticatedController {

    @Inject
    private ExtractorService extractorService;

    @Inject
    private NodeService nodeService;

    @Inject
    private InputService inputService;

    /*
     * This call is just changing the extractor definition in the database
     * and thus does not have to be executed against the node that is actually
     * holding the extractor. This also makes it working with global inputs
     * out of the box. Hooray!
     */
    @BodyParser.Of(BodyParser.Json.class)
    public Result order(String inputId) {
        final JsonNode json = request().body().asJson();

        final SortedMap<Integer, String> positions = Maps.newTreeMap();
        final Iterator<JsonNode> order = json.get("order").elements();
        int i = 0;
        while (order.hasNext()) {
            final String extractorId = order.next().asText();

            positions.put(i, extractorId);

            i++;
        }

        try {
            extractorService.order(inputId, positions);
        } catch (IOException e) {
            Logger.error("Could not update extractor order.", e);
            return internalServerError();
        } catch (APIException e) {
            Logger.error("Could not update extractor order.", e);
            return internalServerError();
        }

        return ok();
    }
}
