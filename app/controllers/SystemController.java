package controllers;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import lib.APIException;
import models.Core;
import play.mvc.Result;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class SystemController extends AuthenticatedController {

    public static Result fields() {
        try {
            Set<String> fields = Core.getMessageFields();

            Map<String, Set<String>> result = Maps.newHashMap();
            result.put("fields", fields);

            return ok(new Gson().toJson(result)).as("application/json");
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        }
    }

}
