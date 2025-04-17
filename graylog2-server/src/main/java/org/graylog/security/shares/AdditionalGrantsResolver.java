package org.graylog.security.shares;

import org.apache.commons.lang.NotImplementedException;
import org.graylog.grn.GRN;

import java.util.Collection;

public interface AdditionalGrantsResolver {
    default Collection<GRN> dependentEntities(GRN ownedEntity) {
        throw new NotImplementedException();
    }
}
