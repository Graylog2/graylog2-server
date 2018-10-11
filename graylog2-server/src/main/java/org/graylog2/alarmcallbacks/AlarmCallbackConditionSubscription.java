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
package org.graylog2.alarmcallbacks;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * This describes alert condition subscriptions for an alarm callback.
 * <p>
 * A subscription can be one of the following types:
 * <ul>
 * <li>ALL - The callback is subscribed to all alert conditions
 * <li>NONE - The callback is not subscribed to any alert condition (it's disabled)
 * <li>SELECTION - The callback is subscribed to a selection of alert conditions
 * </ul>
 */
@AutoValue
@JsonDeserialize(builder = AlarmCallbackConditionSubscription.Builder.class)
public abstract class AlarmCallbackConditionSubscription {
    private static final String FIELD_SUBSCRIPTION_TYPE = "subscription_type";
    private static final String FIELD_SELECTED_ALERT_CONDITIONS = "selected_alert_conditions";

    public enum SubscriptionType {
        ALL, NONE, SELECTION
    }

    @JsonProperty(FIELD_SUBSCRIPTION_TYPE)
    public abstract SubscriptionType subscriptionType();

    @JsonProperty(FIELD_SELECTED_ALERT_CONDITIONS)
    public abstract Set<String> selectedAlertConditions();

    public static AlarmCallbackConditionSubscription all() {
        return builder().subscriptionType(SubscriptionType.ALL).build();
    }

    public static AlarmCallbackConditionSubscription none() {
        return builder().subscriptionType(SubscriptionType.NONE).build();
    }

    public static AlarmCallbackConditionSubscription selection(Set<String> selectedAlertConditions) {
        return builder()
                .subscriptionType(SubscriptionType.SELECTION)
                .selectedAlertConditions(requireNonNull(selectedAlertConditions, "alert condition selection cannot be null"))
                .build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_AlarmCallbackConditionSubscription.Builder()
                    // The default subscription type is set to ALL for backwards compatibility with alarm callbacks
                    // which have been created before the concept of subscriptions existed.
                    .subscriptionType(SubscriptionType.ALL)
                    .selectedAlertConditions(Collections.emptySet());
        }

        @JsonProperty(FIELD_SUBSCRIPTION_TYPE)
        public abstract Builder subscriptionType(SubscriptionType subscriptionType);

        @JsonProperty(FIELD_SELECTED_ALERT_CONDITIONS)
        public abstract Builder selectedAlertConditions(Set<String> selectedAlertConditions);

        public abstract AlarmCallbackConditionSubscription build();
    }
}