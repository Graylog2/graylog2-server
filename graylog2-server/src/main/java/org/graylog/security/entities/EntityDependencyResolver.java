package org.graylog.security.entities;

import com.google.common.collect.ImmutableSet;
import org.graylog.grn.GRN;

/**
 * Resolves dependencies for entities identified by GRNs (Global Resource Names).
 * <p>
 * Implementations of this interface provide methods to determine which other entities
 * a given entity depends on.
 */
public interface EntityDependencyResolver {
    /**
     * Resolves the dependencies for the given entity. Dependencies are other entities which require a capability grant
     * for the grantee to be present in order for the grantee to make effective use of the entity being shared with
     * them.
     *
     * @param entity the GRN of the entity whose dependencies should be resolved
     * @return an immutable set of {@link EntityDescriptor} representing the dependencies of the entity
     */
    ImmutableSet<EntityDescriptor> resolve(GRN entity);

    /**
     * Creates an {@link EntityDescriptor} from the given GRN.
     * <p>
     * TODO: this interface is not a good fit for this method, consider moving it.
     *
     * @param entity the GRN of the entity
     * @return an {@link EntityDescriptor} for the given entity
     */
    EntityDescriptor descriptorFromGRN(GRN entity);
}
