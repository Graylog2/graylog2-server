/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.restclient.models;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.restclient.models.api.responses.searches.SavedSearchSummaryResponse;
import org.joda.time.DateTime;

import java.util.Map;

public class SavedSearch {

    public interface Factory {
        public SavedSearch fromSummaryResponse(SavedSearchSummaryResponse sssr);
    }

    private String id;
    private String title;
    private Map<String, Object> query;
    private DateTime createdAt;
    private User creatorUserId;

    @AssistedInject
    private SavedSearch(UserService userService, @Assisted SavedSearchSummaryResponse ssr) {
        this.id = ssr.id;
        this.title = ssr.title;
        this.query = ssr.query;
        this.createdAt = new DateTime(ssr.createdAt);
        this.creatorUserId = userService.load(ssr.creatorUserId);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Map<String, Object> getQuery() {
        return query;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public User getCreatorUserId() {
        return creatorUserId;
    }

}
