package models;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lib.APIException;
import lib.ApiClient;
import models.api.responses.GetMessageResponse;
import models.api.responses.MessageAnalyzeResponse;
import models.api.responses.MessageFieldResponse;
import models.api.results.MessageAnalyzeResult;
import models.api.results.MessageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.cache.Cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.Callable;

@Singleton
public class MessagesService {
    private static final Logger log = LoggerFactory.getLogger(MessagesService.class);
    public static final int MESSAGE_FIELDS_CACHE_TTL = 5; // seconds
    public static final String MESSAGE_FIELDS_CACHE_KEY = "core.message_fields";
    private final ApiClient api;

    @Inject
    private MessagesService(ApiClient api) {
        this.api = api;
    }

    public Set<String> getMessageFields() throws IOException, APIException {
        try {
            return Cache.getOrElse(MESSAGE_FIELDS_CACHE_KEY, new Callable<Set<String>>() {
                @Override
                public Set<String> call() throws Exception {
                    final MessageFieldResponse response = api.get(MessageFieldResponse.class).path("/system/fields").execute();
                    return response.fields;
                }
            }, MESSAGE_FIELDS_CACHE_TTL);
        } catch (IOException e) {
            log.error("Could not load message fields", e);
        } catch (APIException e) {
            log.error("Could not load message fields", e);
        } catch (Exception e) {
            log.error("Unexpected error condition", e);
            throw new IllegalStateException(e);
        }
        return Sets.newHashSet();
    }

    public MessageResult getMessage(String index, String id) throws IOException, APIException {
        final GetMessageResponse r = api.get(GetMessageResponse.class)
                .path("/messages/{0}/{1}", index, id)
                .execute();
		return new MessageResult(r.message, r.index);
	}
	
	public MessageAnalyzeResult analyze(String index, String what) throws IOException, APIException {
		if (what == null || what.isEmpty()) {
			return new MessageAnalyzeResult(new ArrayList<String>());
		}

        MessageAnalyzeResponse r = api.get(MessageAnalyzeResponse.class)
                .path("/messages/{0}/analyze", index)
                .queryParam("string", what)
                .execute();
		return new MessageAnalyzeResult(r.tokens);
	}


}
