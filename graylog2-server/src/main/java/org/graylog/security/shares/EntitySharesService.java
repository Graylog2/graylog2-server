package org.graylog.security.shares;

import org.apache.shiro.subject.Subject;
import org.graylog.grn.GRN;
import org.graylog.security.Capability;
import org.graylog2.plugin.database.users.User;

import java.util.Map;
import java.util.Optional;

public interface EntitySharesService {
    EntityShareResponse prepareShare(Optional<GRN> ownedEntity, EntityShareRequest request, User sharingUser, Subject sharingSubject);

    EntityShareResponse updateEntityShares(GRN ownedEntity, EntityShareRequest request, User sharingUser);

    Map<GRN, Capability> getGrants(GRN ownedEntity);
}
