/*
 * Copyright 2013 TORCH UG
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
 */
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
