/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import usePermissions from 'hooks/usePermissions';

import { COLLECTOR_PERMISSIONS, scoped } from '../Permissions';

/**
 * Permission checks for the Collectors feature.
 *
 * Every fleet-scoped check passes the scoped string (e.g. `collector_fleets:edit:<fleetId>`).
 * `isPermitted` matches an exact grant first and then falls back to the unscoped and wildcard
 * forms, so one expression is correct for admins, role holders and grantees alike.
 */
const useCollectorPermissions = () => {
  const { isPermitted } = usePermissions();

  return {
    canCreateFleet: isPermitted(COLLECTOR_PERMISSIONS.FLEET_CREATE),
    canEditFleet: (fleetId: string) => isPermitted(scoped(COLLECTOR_PERMISSIONS.FLEET_EDIT, fleetId)),
    canDeleteFleet: (fleetId: string) => isPermitted(scoped(COLLECTOR_PERMISSIONS.FLEET_DELETE, fleetId)),
    canCreateSource: (fleetId: string) => isPermitted(scoped(COLLECTOR_PERMISSIONS.SOURCE_CREATE, fleetId)),
    canEditSource: (fleetId: string) => isPermitted(scoped(COLLECTOR_PERMISSIONS.SOURCE_EDIT, fleetId)),
    canDeleteSource: (fleetId: string) => isPermitted(scoped(COLLECTOR_PERMISSIONS.SOURCE_DELETE, fleetId)),
    canDeleteInstance: (fleetId: string) => isPermitted(scoped(COLLECTOR_PERMISSIONS.FLEET_INSTANCE_DELETE, fleetId)),
    // The backend checks BOTH on the target fleet (CollectorInstancesResource:347-348).
    canAssignToFleet: (fleetId: string) =>
      isPermitted([
        scoped(COLLECTOR_PERMISSIONS.FLEET_READ, fleetId),
        scoped(COLLECTOR_PERMISSIONS.FLEET_INSTANCE_ASSIGN, fleetId),
      ]),
    canCreateToken: (fleetId: string) => isPermitted(scoped(COLLECTOR_PERMISSIONS.ENROLL_TOKEN_CREATE, fleetId)),
    canDeleteToken: (fleetId: string) => isPermitted(scoped(COLLECTOR_PERMISSIONS.ENROLL_TOKEN_DELETE, fleetId)),
    canReadActivities: isPermitted(COLLECTOR_PERMISSIONS.ACTIVITIES_READ),
    canEditConfig: isPermitted(COLLECTOR_PERMISSIONS.CONFIGURATION_EDIT),
  };
};

export default useCollectorPermissions;
