/**
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
 */
package org.graylog2.restclient.models;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.Configuration;
import org.graylog2.restclient.models.api.responses.GetMessageResponse;
import org.graylog2.restclient.models.api.responses.HighlightRange;
import org.graylog2.restclient.models.api.responses.MessageAnalyzeResponse;
import org.graylog2.restclient.models.api.responses.MessageCountResponse;
import org.graylog2.restclient.models.api.responses.MessageFieldResponse;
import org.graylog2.restclient.models.api.results.MessageAnalyzeResult;
import org.graylog2.restclient.models.api.results.MessageResult;
import org.graylog2.restroutes.generated.routes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.cache.Cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@Singleton
public class MessagesService {
    private static final Logger log = LoggerFactory.getLogger(MessagesService.class);

    public static final int MESSAGE_FIELDS_CACHE_TTL = 5; // seconds
    public static final String MESSAGE_FIELDS_CACHE_KEY = "core.message_fields";

    public static final int TOTAL_CNT_CACHE_TTL = 2; // seconds
    public static final String TOTAL_CNT_CACHE_KEY = "counts.total";

    private final ApiClient api;
    private final FieldMapper fieldMapper;

    @Inject
    private MessagesService(ApiClient api, FieldMapper fieldMapper) {
        this.api = api;
        this.fieldMapper = fieldMapper;
    }

    public Set<String> getMessageFields() {
        try {
            return Cache.getOrElse(MESSAGE_FIELDS_CACHE_KEY, new Callable<Set<String>>() {
                @Override
                public Set<String> call() throws Exception {
                    final MessageFieldResponse response = api.path(routes.SystemResource().fields(), MessageFieldResponse.class)
                            .queryParam("limit", Configuration.getFieldListLimit())
                            .execute();
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
        return Collections.emptySet();
    }

    public long total() {
        try {
            return Cache.getOrElse(TOTAL_CNT_CACHE_KEY, new Callable<Long>() {
                @Override
                public Long call() throws Exception {
                    MessageCountResponse response = api.path(routes.CountResource().total(), MessageCountResponse.class).execute();
                    return response.events;
                }
            }, TOTAL_CNT_CACHE_TTL);
        } catch (IOException e) {
            log.error("Could not load total message count", e);
        } catch (APIException e) {
            log.error("Could not load total message count", e);
        } catch (Exception e) {
            log.error("Unexpected error condition", e);
            throw new IllegalStateException(e);
        }
        return 0;
    }
    public MessageResult getMessage(String index, String id) throws IOException, APIException {
        final GetMessageResponse r = api.path(routes.MessageResource().search(index, id), GetMessageResponse.class)
                .execute();
		return new MessageResult(r.message, r.index, Maps.<String, List<HighlightRange>>newHashMap(), fieldMapper);
	}
	
	public MessageAnalyzeResult analyze(String index, String what) throws IOException, APIException {
		if (what == null || what.isEmpty()) {
			return new MessageAnalyzeResult(new ArrayList<String>());
		}

        MessageAnalyzeResponse r = api.path(routes.MessageResource().analyze(index), MessageAnalyzeResponse.class)
                .queryParam("string", what)
                .execute();
		return new MessageAnalyzeResult(r.tokens);
	}

}
