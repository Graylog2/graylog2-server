package models;

import lib.APIException;
import lib.ApiClient;
import models.api.responses.MessageCountResponse;
import models.api.results.MessageCountResult;
import play.cache.Cache;

import java.io.IOException;
import java.util.concurrent.Callable;

public class MessageCount {

    public static final int TOTAL_CNT_CACHE_TTL = 2; // seconds
    public static final String TOTAL_CNT_CACHE_KEY = "counts.total";

    public MessageCountResult total() throws IOException, APIException {
        try {
            return Cache.getOrElse(TOTAL_CNT_CACHE_KEY, new Callable<MessageCountResult>() {
                @Override
                public MessageCountResult call() throws Exception {
                    MessageCountResponse response = ApiClient.get(MessageCountResponse.class).path("/count/total").execute();
                    return new MessageCountResult(response.events);
                }
            }, TOTAL_CNT_CACHE_TTL);
        } catch (IOException e) {
            throw e;
        } catch (APIException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
