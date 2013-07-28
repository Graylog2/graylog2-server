package models;

import lib.APIException;
import lib.Api;
import models.api.responses.MessageCountResponse;
import models.api.results.MessageCountResult;
import play.cache.Cache;

import java.io.IOException;

public class MessageCount {

    public static final int TOTAL_CNT_CACHE_TTL = 2; // seconds
    public static final String TOTAL_CNT_CACHE_KEY = "counts.total";

    public MessageCountResult total() throws IOException, APIException {
        MessageCountResult cached = (MessageCountResult) Cache.get(TOTAL_CNT_CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        MessageCountResponse response = Api.get("count/total", MessageCountResponse.class);
        MessageCountResult result = new MessageCountResult(response.events);
        Cache.set(TOTAL_CNT_CACHE_KEY, result, TOTAL_CNT_CACHE_TTL);
        return result;
    }

}
