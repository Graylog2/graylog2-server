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

package org.graylog2.utilities;

import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;

import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

/**
 * This is a helper class for GRNs - Graylog Resource Names
 * GRNs are like URNs that we use for internal purposes only
 * <p>
 * <pre>
 * GRN format:
 *   {@literal grn:<cluster>:<tenant>:<scope>:<type>:<entity>}
 * Examples:
 * grn::::stream:000000000001
 * grn:local:0:internal:stream:000000000001
 * </pre>
 * </p>
 */
@AutoValue
public abstract class GRN {
    public abstract String cluster();

    private static final Splitter SPLITTER = Splitter.on(":").trimResults();
    public abstract String tenant();
    public abstract String scope();
    public abstract String type();
    public abstract String entity();

    public static GRN parse(String grn) {
        final List<String> tokens = SPLITTER.splitToList(grn.toLowerCase(Locale.ENGLISH));

        if (tokens.size() != 6) {
            throw new IllegalArgumentException("not a valid GRN string");
        }
        if (!tokens.get(0).equals("grn")) {
            throw new IllegalArgumentException(String.format(Locale.US, "<%s> is not a grn scheme", tokens.get(0)));
        }
        final Builder builder = builder()
                .cluster(tokens.get(1))
                .tenant(tokens.get(2))
                .scope(tokens.get(3))
                .type(tokens.get(4))
                .entity(tokens.get(5));

        return builder.build();
    }

    public static Builder builder() {
        return new AutoValue_GRN.Builder().cluster("").tenant("").scope("");
    }

    @Override
    public String toString() {
        final StringJoiner joiner = new StringJoiner(":");
        joiner.add("grn")
                .add(cluster())
                .add(tenant())
                .add(scope())
                .add(type())
                .add(entity());

        return joiner.toString();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract GRN build();

        public abstract Builder cluster(String cluster);
        public abstract Builder tenant(String tenant);
        public abstract Builder scope(String scope);
        public abstract Builder type(String type);
        public abstract Builder entity(String entity);
    }
}
