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
package org.graylog2.dashboards;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.dashboards.widgets.WidgetPosition;

import javax.annotation.Nullable;
import java.util.Collection;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class DashboardDTO {
    public static final String FIELD_ID = "_id";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_CONTENT_PACK = "content_pack";
    public static final String FIELD_CREATOR_USER_ID = "creator_user_id";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String EMBEDDED_WIDGETS = "widgets";
    public static final String EMBEDDED_POSITIONS = "positions";

    @JsonProperty("id")
    public abstract String id();

    @JsonProperty(FIELD_TITLE)
    public abstract String title();

    @JsonProperty(FIELD_DESCRIPTION)
    public abstract String description();

    @JsonProperty(FIELD_CREATOR_USER_ID)
    public abstract String creatorUserId();

    @JsonProperty(FIELD_CREATED_AT)
    public abstract String createdAt();

    @JsonProperty(FIELD_CONTENT_PACK)
    @Nullable
    public abstract String contentPack();

    @JsonProperty(EMBEDDED_WIDGETS)
    @Nullable
    public abstract Collection<DashboardWidgetDTO> widgets();

    @JsonProperty(EMBEDDED_POSITIONS)
    @Nullable
    public abstract Collection<WidgetPosition> positions();

    public abstract Builder toBuilder();

    @JsonCreator
    public static DashboardDTO create(@JsonProperty(FIELD_ID) String id,
                        @JsonProperty(FIELD_TITLE) String title,
                        @JsonProperty(FIELD_DESCRIPTION) String description,
                        @JsonProperty(FIELD_CREATOR_USER_ID) String creatorUserId,
                        @JsonProperty(FIELD_CREATED_AT) String createdAt,
                        @JsonProperty(FIELD_CONTENT_PACK) @Nullable String contentPack,
                        @JsonProperty(EMBEDDED_WIDGETS) @Nullable Collection<DashboardWidgetDTO> widgets,
                        @JsonProperty(EMBEDDED_POSITIONS) @Nullable Collection<WidgetPosition> positions) {
        return new AutoValue_DashboardDTO(
                id,
                title,
                description,
                creatorUserId,
                createdAt,
                contentPack,
                widgets,
                positions);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator Builder create() { return new AutoValue_DashboardDTO.Builder(); }

        @JsonProperty("id")
        public abstract Builder id(String id);

        @JsonProperty(FIELD_TITLE)
        public abstract Builder title(String title);

        @JsonProperty(FIELD_DESCRIPTION)
        public abstract Builder description(String description);

        @JsonProperty(FIELD_CONTENT_PACK)
        public abstract Builder contentPack(String contentPack);

        @JsonProperty(FIELD_CREATOR_USER_ID)
        public abstract Builder creatorUserId(String creatorUserId);

        @JsonProperty(FIELD_CREATED_AT)
        public abstract Builder createdAt(String createdAt);

        @JsonProperty(EMBEDDED_WIDGETS)
        public abstract Builder widgets(Collection<DashboardWidgetDTO> widgets);

        @JsonProperty(EMBEDDED_POSITIONS)
        public abstract Builder positions(Collection<WidgetPosition> positions);

        public abstract DashboardDTO build();
    }
}
