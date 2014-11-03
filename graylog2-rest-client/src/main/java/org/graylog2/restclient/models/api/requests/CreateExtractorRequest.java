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
package org.graylog2.restclient.models.api.requests;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class CreateExtractorRequest extends ApiRequest {

    public String title;

    @SerializedName("cut_or_copy")
    public String cutOrCopy;

    @SerializedName("target_field")
    public String targetField;

    @SerializedName("source_field")
    public String sourceField;

    @SerializedName("extractor_type")
    public String extractorType;

    @SerializedName("creator_user_id")
    public String creatorUserId;

    @SerializedName("extractor_config")
    public Map<String, Object> extractorConfig;

    public Map<String, Map<String, Object>> converters;

    @SerializedName("condition_type")
    public String conditionType;

    @SerializedName("condition_value")
    public String conditionValue;

    public int order;

}
