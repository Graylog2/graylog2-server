package org.graylog.security.shares;

import org.apache.commons.lang.NotImplementedException;
import org.graylog.grn.GRN;

import java.util.Collection;

public interface AdditionalGrantsResolver {
    /**
     * Return dependent entities that need to kept in sync with the primary entity.
     *
     * @param primaryEntity The primary entity
     * @return A collection of dependent entities
     */
    default Collection<GRN> dependentEntities(GRN primaryEntity) {
        throw new NotImplementedException();
    }
}
