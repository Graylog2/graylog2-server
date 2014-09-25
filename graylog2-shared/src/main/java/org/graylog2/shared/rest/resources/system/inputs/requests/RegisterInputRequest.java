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
package org.graylog2.shared.rest.resources.system.inputs.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.plugin.inputs.MessageInput;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class RegisterInputRequest {

    @JsonProperty("input_id")
    public String inputId;

    public String title;
    public String type;

    public Map<String, Object> configuration;

    @JsonProperty("radio_id")
    public String radioId;

    @JsonProperty("creator_user_id")
    public String creatorUserId;

    public RegisterInputRequest() {
    }

    public RegisterInputRequest(MessageInput input, String radioId) {
        this.inputId = input.getId();
        this.title = input.getTitle();
        this.type = input.getClass().getCanonicalName();
        this.configuration = input.getConfiguration().getSource();
        this.radioId = radioId;
        this.creatorUserId = input.getCreatorUserId();
    }

}
