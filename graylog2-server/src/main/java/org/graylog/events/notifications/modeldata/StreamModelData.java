package org.graylog.events.notifications.modeldata;

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
