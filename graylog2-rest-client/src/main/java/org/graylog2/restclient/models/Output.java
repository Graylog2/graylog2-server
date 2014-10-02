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
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.responses.system.OutputSummaryResponse;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class Output {
    private final ApiClient api;
    private final UserService userService;

    public interface Factory {
        Output fromSummaryResponse(OutputSummaryResponse outputSummaryResponse);
    }
    private final String title;
    private final String _id;
    private final String creatorUserId;
    private final Map<String, Object> configuration;
    private final String type;
    private final String createdAt;

    @AssistedInject

    public Output(ApiClient api,
                  UserService userService,
                  @Assisted OutputSummaryResponse outputSummaryResponse) {
        this.api = api;
        this.userService = userService;

        this.title = outputSummaryResponse.title;
        this._id = outputSummaryResponse._id;
        this.creatorUserId = outputSummaryResponse.creatorUserId;
        this.configuration = outputSummaryResponse.configuration;
        this.type = outputSummaryResponse.type;
        this.createdAt = outputSummaryResponse.createdAt;
    }

    public ApiClient getApi() {
        return api;
    }

    public String getTitle() {
        return title;
    }

    public String getId() {
        return _id;
    }

    public String getCreatorUserId() {
        return creatorUserId;
    }

    public User getCreatorUser() {
        return userService.load(getCreatorUserId());
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public String getType() {
        return type;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Output)) return false;

        Output output = (Output) o;

        if (!_id.equals(output._id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return _id.hashCode();
    }
}
