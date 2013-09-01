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
package controllers;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import lib.APIException;
import models.UniversalSearch;
import models.api.responses.FieldStatsResponse;
import play.Logger;
import play.mvc.Result;

import java.io.IOException;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SearchApiController extends AuthenticatedController {

    public static Result fieldStats(String q, String timerange, String field) {
        int range;
        try {
            range = Integer.parseInt(timerange);
        } catch (NumberFormatException e) {
            Logger.warn("Could not parse timerange. Setting to 0.");
            range = 0;
        }

        try {
            UniversalSearch search = new UniversalSearch(q, range);
            FieldStatsResponse stats = search.fieldStats(field);

            Map<String, Object> result = Maps.newHashMap();
            result.put("count", stats.count);
            result.put("sum", stats.sum);
            result.put("mean", stats.mean);
            result.put("min", stats.min);
            result.put("max", stats.max);
            result.put("variance", stats.variance);
            result.put("sum_of_squares", stats.sumOfSquares);
            result.put("std_deviation", stats.stdDeviation);

            return ok(new Gson().toJson(result)).as("application/json");
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            if (e.getHttpCode() == 400) {
                // This usually means the field does not have a numeric type. Pass through!
                return badRequest();
            }

            return internalServerError("api exception " + e);
        }
    }

}
