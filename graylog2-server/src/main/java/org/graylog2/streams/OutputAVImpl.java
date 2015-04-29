/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.streams;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.database.CollectionName;
import org.graylog2.plugin.streams.Output;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Map;

@AutoValue
@JsonAutoDetect
@CollectionName("outputs")
public abstract class OutputAVImpl implements Output {
    @Override
    @JsonProperty("_id")
    @ObjectId
    public abstract String getId();

    @Override
    @JsonProperty("title")
    public abstract String getTitle();

    @Override
    @JsonProperty("type")
    public abstract String getType();

    @Override
    @JsonProperty("creator_user_id")
    public abstract String getCreatorUserId();

    @Override
    @JsonProperty("configuration")
    public abstract Map<String, Object> getConfiguration();

    @Override
    @JsonProperty("created_at")
    public abstract Date getCreatedAt();

    @Override
    @JsonProperty("content_pack")
    @Nullable
    public abstract String getContentPack();

    @JsonCreator
    public static OutputAVImpl create(@JsonProperty("_id") String _id,
                                    @JsonProperty("title") String title,
                                    @JsonProperty("type") String type,
                                    @JsonProperty("creator_user_id") String creator_user_id,
                                    @JsonProperty("configuration") Map<String, Object> configuration,
                                    @JsonProperty("created_at") Date created_at,
                                    @JsonProperty("content_pack") @Nullable String content_pack) {
        return new AutoValue_OutputAVImpl(_id, title, type, creator_user_id, configuration, created_at, content_pack);

    }
}
