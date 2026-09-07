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
import type { Permission } from 'graylog-web-plugin/plugin';

/**
 * Mirrors org.graylog.collectors.CollectorsPermissions. These strings are a contract with the
 * backend — they must match byte-for-byte.
 *
 * Fleet, source, instance and token permissions are entity-scoped to the *fleet*: the backend
 * checks them as `<permission>:<fleetId>`. Sources and instances do not own their permission;
 * their parent fleet does. Use `scoped()` to build the check.
 */
export const COLLECTOR_PERMISSIONS = {
  FLEET_CREATE: 'collector_fleets:create' as Permission,
  FLEET_READ: 'collector_fleets:read' as Permission,
  FLEET_EDIT: 'collector_fleets:edit' as Permission,
  FLEET_DELETE: 'collector_fleets:delete' as Permission,
  FLEET_INSTANCE_ASSIGN: 'collector_fleets:assign_instance' as Permission,
  FLEET_INSTANCE_DELETE: 'collector_fleets:delete_instance' as Permission,
  SOURCE_CREATE: 'collector_fleets:source_create' as Permission,
  SOURCE_EDIT: 'collector_fleets:source_edit' as Permission,
  SOURCE_DELETE: 'collector_fleets:source_delete' as Permission,
  ENROLL_TOKEN_CREATE: 'collector_enrollment_tokens:create' as Permission,
  ENROLL_TOKEN_READ: 'collector_enrollment_tokens:read' as Permission,
  ENROLL_TOKEN_DELETE: 'collector_enrollment_tokens:delete' as Permission,
  CONFIGURATION_READ: 'collectors_config:read' as Permission,
  CONFIGURATION_EDIT: 'collectors_config:edit' as Permission,
  ACTIVITIES_READ: 'collector_activities:read' as Permission,
} as const;

export const scoped = (permission: string, entityId: string) => `${permission}:${entityId}` as Permission;
