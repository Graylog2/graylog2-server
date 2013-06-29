package models;

import lib.APIException;
import lib.Api;
import models.api.responses.MessageFieldResponse;
import play.cache.Cache;

import java.io.IOException;
import java.util.Set;

public class Core {

    public static final int MESSAGE_FIELDS_CACHE_TTL = 5; // seconds
    public static final String MESSAGE_FIELDS_CACHE_KEY = "core.message_fields";

    public static Set<String> getMessageFields() throws IOException, APIException {
        Set<String> cached = (Set<String>) Cache.get(MESSAGE_FIELDS_CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        MessageFieldResponse response = Api.get("system/fields", MessageFieldResponse.class);
        Set<String> result = response.fields;
        Cache.set(MESSAGE_FIELDS_CACHE_KEY, result, MESSAGE_FIELDS_CACHE_TTL);
        return result;
    }

}
