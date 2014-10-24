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
    private final String contentPack;

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
        this.contentPack = outputSummaryResponse.contentPack;
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

    public String getContentPack() {
        return contentPack;
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
