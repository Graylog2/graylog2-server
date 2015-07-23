package org.graylog2.shared.users;

import java.util.Set;

public interface Role {
    String getId();

    String getName();

    void setName(String name);

    Set<String> getPermissions();

    void setPermissions(Set<String> permissions);
}
