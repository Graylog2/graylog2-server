package models;

import lib.APIException;
import lib.ApiClient;
import models.api.responses.MessageFieldResponse;
import play.cache.Cache;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Callable;

public class Core {

    public static final int MESSAGE_FIELDS_CACHE_TTL = 5; // seconds
    public static final String MESSAGE_FIELDS_CACHE_KEY = "core.message_fields";

    public static Set<String> getMessageFields() throws IOException, APIException {
        try {
            return Cache.getOrElse(MESSAGE_FIELDS_CACHE_KEY, new Callable<Set<String>>() {
                @Override
                public Set<String> call() throws Exception {
                    final MessageFieldResponse response = ApiClient.get(MessageFieldResponse.class).path("/system/fields").execute();
                    return response.fields;
                }
            }, MESSAGE_FIELDS_CACHE_TTL);
        } catch (IOException e) {
            throw e;
        } catch (APIException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
