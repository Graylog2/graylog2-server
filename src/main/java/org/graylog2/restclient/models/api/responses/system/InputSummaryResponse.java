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
package org.graylog2.restclient.models.api.responses.system;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class InputSummaryResponse {

    public String type;

    @SerializedName("input_id")
    public String inputId;

    @SerializedName("persist_id")
    public String persistId;

    public String name;
    public String title;

    @SerializedName("creator_user_id")
    public String creatorUserId;

    @SerializedName("created_at")
    public String createdAt;

    public Boolean global;

    @SerializedName("static_fields")
    public Map<String, String> staticFields;

    public Map<String, Object> attributes;

}
