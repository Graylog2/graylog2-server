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

import com.google.common.collect.ImmutableSet;
import org.graylog2.plugin.database.users.User;

import javax.inject.Singleton;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * The global {@link GRN} registry.
 */
@Singleton
public class GRNRegistry {
    public static final String GLOBAL_USER_GRN = "grn::::builtin-team:everyone"; // TODO: Find a better name for the "everyone" grantee GRN type

    // TODO This is essentially the same as org.graylog2.contentpacks.model.ModelTypes
    // TODO find a way to unify these
    private static final ImmutableSet<GRNType> BUILTIN_TYPES = ImmutableSet.<GRNType>builder()
            .add(GRNType.create("collection", "collections:"))
            .add(GRNType.create("dashboard", "dashboards:"))
            .add(GRNType.create("grant", "grants:"))
            .add(GRNType.create("role", "roles:"))
            .add(GRNType.create("stream", "streams:"))
            .add(GRNType.create("user", "users:"))
            .add(GRNType.create("team", "teams:"))
            .add(GRNType.create("builtin-team", "XXX-NOT-A-REAL-TYPE-XXX:"))
            .build();

    private final ConcurrentMap<String, GRNType> REGISTRY = new ConcurrentHashMap<>();

    // Don't allow direct instantiation
    private GRNRegistry() {
    }

    /**
     * Returns an empty registry.
     *
     * @return the registry
     */
    public static GRNRegistry createEmpty() {
        return new GRNRegistry();
    }

    /**
     * Returns a registry that has been initialized with the builtin Graylog GRN types.
     *
     * @return the registry
     */
    public static GRNRegistry createWithBuiltinTypes() {
        return createWithTypes(BUILTIN_TYPES);
    }

    /**
     * Returns a registry that has been initialized with the given GRN types.
     *
     * @param types the GRN types to initialize the registry with
     * @return the registry
     */
    public static GRNRegistry createWithTypes(Collection<GRNType> types) {
        final GRNRegistry grnRegistry = new GRNRegistry();

        types.forEach(grnRegistry::registerType);

        return grnRegistry;
    }

    /**
     * Parses the given GRN string and returns a {@link GRN}.
     *
     * @param grnString the GRN string to parse
     * @return the GRN
     * @throws IllegalArgumentException when given GRN string is invalid
     */
    public GRN parse(String grnString) {
        return GRN.parse(grnString, this);
    }

    /**
     * Returns the {@link GRN} for the given type and entity.
     *
     * @param type   the GRN type string
     * @param entity the entity string
     * @return the GRN
     * @throws IllegalArgumentException when given type doesn't exist or any arguments are null or empty
     */
    public GRN newGRN(String type, String entity) {
        checkArgument(!isNullOrEmpty(type), "type cannot be null or empty");
        checkArgument(!isNullOrEmpty(entity), "entity cannot be null or empty");

        return newGRNBuilder(type).entity(entity).build();
    }

    public GRN ofUser(User user) {
        return newGRN("user", user.getName());
    }

    /**
     * Returns a new {@link GRN.Builder} for the given type string.
     *
     * @param type the GRN type string
     * @return the GRN builder
     * @throws IllegalArgumentException when given type doesn't exist
     */
    public GRN.Builder newGRNBuilder(String type) {
        final GRNType grnType = Optional.ofNullable(REGISTRY.get(toKey(type)))
                .orElseThrow(() -> new IllegalArgumentException("type <" + type + "> does not exist"));

        return grnType.newGRNBuilder();
    }

    /**
     * Registers the given GRN type.
     *
     * @param type the typt to register
     * @throws IllegalStateException when given type is already registered
     */
    public void registerType(GRNType type) {
        checkArgument(type != null, "type cannot be null");

        if (REGISTRY.putIfAbsent(toKey(type.type()), type) != null) {
            throw new IllegalStateException("Type <" + type.type() + "> already exists");
        }
    }

    private String toKey(String type) {
        checkArgument(type != null, "type cannot be null");
        checkArgument(!type.trim().isEmpty(), "type name cannot be empty");

        return type.trim().toLowerCase(Locale.US);
    }
}
