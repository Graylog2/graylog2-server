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
package org.graylog.integrations.notifications.modeldata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class StreamModelData {
	@JsonProperty("id")
	public abstract String id();

	@JsonProperty("title")
	public abstract String title();

	@JsonProperty("description")
	public abstract String description();

	@JsonProperty("url")
	public abstract String url();

	public static Builder builder() {
		return new AutoValue_StreamModelData.Builder();
	}

	public abstract Builder toBuilder();

	@AutoValue.Builder
	public static abstract class Builder {

		public abstract Builder id(String id);

		public abstract Builder title(String title);

		public abstract Builder description(String description);

		public abstract Builder url(String url);

		public abstract StreamModelData build();
	}
}
