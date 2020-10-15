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
package org.graylog.security.events;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import org.graylog.grn.GRN;
import org.graylog.security.Capability;
import org.graylog2.plugin.database.users.User;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@AutoValue
@JsonAutoDetect
public abstract class EntitySharesUpdateEvent {
    public abstract User user();
    public abstract GRN entity();
    public abstract ImmutableList<Share> creates();
    public abstract ImmutableList<Share> deletes();
    public abstract ImmutableList<Share> updates();

    public static EntitySharesUpdateEvent create(User user, GRN entity, List<Share> creates, List<Share> deletes, List<Share> updates) {
        return builder()
                .user(user)
                .entity(entity)
                .creates(creates)
                .deletes(deletes)
                .updates(updates)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_EntitySharesUpdateEvent.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract ImmutableList.Builder<Share> createsBuilder();
        public abstract ImmutableList.Builder<Share> deletesBuilder();
        public abstract ImmutableList.Builder<Share> updatesBuilder();

        public abstract Builder user(User user);

        public abstract Builder entity(GRN entity);

        public Builder addCreates(GRN grantee, Capability capability) {
            createsBuilder().add(Share.create(grantee, capability, null));
            return this;
        }
        public Builder addDeletes(GRN grantee, Capability capability) {
            deletesBuilder().add(Share.create(grantee, capability, null));
            return this;
        }
        public Builder addUpdates(GRN grantee, Capability capability, Capability formerCapability) {
            updatesBuilder().add(Share.create(grantee, capability, formerCapability));
            return this;
        }
        public abstract Builder creates(List<Share> creates);
        public abstract Builder deletes(List<Share> deletes);
        public abstract Builder updates(List<Share> updates);

        public abstract EntitySharesUpdateEvent build();

    }

    @AutoValue
    @JsonAutoDetect
    public abstract static class Share {
        public abstract GRN grantee();
        public abstract Capability capability();
        public abstract Optional<Capability> formerCapability();

        public static Share create(GRN grantee, Capability capability, @Nullable Capability formerCapability) {
            return new AutoValue_EntitySharesUpdateEvent_Share(grantee, capability, Optional.ofNullable(formerCapability));
        }

    }
}
