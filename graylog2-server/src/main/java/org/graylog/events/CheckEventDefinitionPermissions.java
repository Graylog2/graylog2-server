package org.graylog.events;

import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.plugins.views.search.errors.PermissionException;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog.security.UserContext;

import static org.graylog2.shared.security.RestPermissions.EVENT_DEFINITIONS_CREATE;
import static org.graylog2.shared.security.RestPermissions.EVENT_DEFINITIONS_DELETE;
import static org.graylog2.shared.security.RestPermissions.EVENT_DEFINITIONS_EDIT;
import static org.graylog2.shared.security.RestPermissions.EVENT_DEFINITIONS_EXECUTE;
import static org.graylog2.shared.security.RestPermissions.EVENT_DEFINITIONS_READ;

public class CheckEventDefinitionPermissions {
    private final UserContext userContext;
    private final DBGrantService dbGrantService;
    private final GRNRegistry grnRegistry;

    private CheckEventDefinitionPermissions(final UserContext userContext, final DBGrantService dbGrantService, final GRNRegistry grnRegistry) {
        this.userContext = userContext;
        this.dbGrantService = dbGrantService;
        this.grnRegistry = grnRegistry;
    }

    public static CheckEventDefinitionPermissions create(final UserContext userContext, final DBGrantService dbGrantService, final GRNRegistry grnRegistry) {
        return new CheckEventDefinitionPermissions(userContext, dbGrantService, grnRegistry);
    }

    // check for nonEmpty, as the minimal grant is view capability
    private boolean readAllowedOnEntity(final String id) {
        final var targetGRN = grnRegistry.newGRN(GRNTypes.EVENT_DEFINITION, id);
        final var userGRN = grnRegistry.newGRN(GRNTypes.USER, userContext.getUser().getId());
        return !dbGrantService.getForTargetAndGrantee(targetGRN, userGRN).isEmpty();
    }

    private boolean manageAllowedOnEntity(final String id) {
        final var targetGRN = grnRegistry.newGRN(GRNTypes.EVENT_DEFINITION, id);
        final var userGRN = grnRegistry.newGRN(GRNTypes.USER, userContext.getUser().getId());
        final var grants = dbGrantService.getForTargetAndGrantee(targetGRN, userGRN);
        return grants.stream().anyMatch(g -> g.capability().priority() >= Capability.MANAGE.priority());
    }

    public void checkRead(final String id) {
        if(!canRead(id)) {
            throw new PermissionException("Action not allowed");
        }
    }

    public boolean canRead(final String id) {
        return userContext.isPermitted(EVENT_DEFINITIONS_READ, id) || readAllowedOnEntity(id);
    }

    public boolean canRead(EventDefinitionDto eventDefinitionDto) {
        return canRead(eventDefinitionDto.id());
    }

    public void checkCreate(final String id) {
        if(!canCreate()) {
            throw new PermissionException("Action not allowed");
        }
    }

    public boolean canCreate() {
        return userContext.isPermitted(EVENT_DEFINITIONS_CREATE);
    }

    public void checkEdit(final String id) {
        if(!canEdit(id)) {
            throw new PermissionException("Action not allowed");
        }
    }

    public boolean canEdit(final String id) {
        return userContext.isPermitted(EVENT_DEFINITIONS_EDIT, id) || manageAllowedOnEntity(id);
    }

    public void checkExecute(final String id) {
        if(!canExecute(id)) {
            throw new PermissionException("Action not allowed");
        }
    }

    public boolean canExecute(final String id) {
        return userContext.isPermitted(EVENT_DEFINITIONS_EXECUTE, id) || manageAllowedOnEntity(id);
    }

    public void checkDelete(final String id) {
        if(!canDelete(id)) {
            throw new PermissionException("Action not allowed");
        }
    }

    public boolean canDelete(final String id) {
        return userContext.isPermitted(EVENT_DEFINITIONS_DELETE, id) || manageAllowedOnEntity(id);
    }
}
