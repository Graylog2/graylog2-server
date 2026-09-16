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
import { COLLECTOR_SYSTEM_LOGS_STREAM_ID } from '../common/fields';

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
    // Page- and tab-level gates. Unlike the checks above these are unscoped, because a page has no
    // single fleet in context — the scoped variants still gate the individual controls inside it.
    // Consequence: a user holding only a fleet-scoped grant would not match here. That mirrors the
    // existing top-level Collectors nav gate, and is unreachable today (fleets are not shareable).
    canDeployCollectors: isPermitted(COLLECTOR_PERMISSIONS.ENROLL_TOKEN_CREATE),
    canViewEnrollmentTokens: isPermitted(COLLECTOR_PERMISSIONS.ENROLL_TOKEN_READ),
    // Collector self-logs are routed to a built-in stream, so the links into search are gated by a
    // STREAM permission rather than a collector one. Without it the link lands on the "Missing
    // Stream Permissions" page, so we know enough not to offer it.
    canReadSystemLogs: isPermitted(scoped('streams:read', COLLECTOR_SYSTEM_LOGS_STREAM_ID)),
    canReadActivities: isPermitted(COLLECTOR_PERMISSIONS.ACTIVITIES_READ),
    canEditConfig: isPermitted(COLLECTOR_PERMISSIONS.CONFIGURATION_EDIT),
  };
};

export default useCollectorPermissions;
