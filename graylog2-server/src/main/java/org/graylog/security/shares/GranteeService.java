package org.graylog.security.shares;

import org.graylog.security.shares.EntitySharePrepareResponse.AvailableGrantee;
import org.graylog2.plugin.database.users.User;

import java.util.Set;

public interface GranteeService {
    Set<AvailableGrantee> getAvailableGrantees(User sharingUser);
}
