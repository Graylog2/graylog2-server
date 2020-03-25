package org.graylog2.shared.rest.exceptions;

import java.util.Set;

public class MissingStreamPermissionException extends RuntimeException {
    private Set<String> streamsWithMissingPermissions;

    public MissingStreamPermissionException(String errorMessage, Set<String> streamsWithMissingPermissions) {
        super(errorMessage);
        this.streamsWithMissingPermissions = streamsWithMissingPermissions;
    }

    public Set<String> streamsWithMissingPermissions() {
        return this.streamsWithMissingPermissions;
    }
}
