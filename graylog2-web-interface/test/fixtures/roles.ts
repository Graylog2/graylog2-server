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
// @flow strict
import * as Immutable from 'immutable';

import Role from 'logic/roles/Role';

export const manager = Role.builder()
  .id('manager-id')
  .name('Manager')
  .description('Role description')
  .permissions(Immutable.Set(['dashboards:read']))
  .readOnly(true)
  .build();

export const alertsManager = Role.builder()
  .id('alerts-manager-id')
  .name('Alerts Manager')
  .description('Allows reading and writing all event definitions and event notifications (built-in)')
  .permissions(Immutable.Set(['eventdefinitions:read', 'eventdefinitions:create']))
  .readOnly(true)
  .build();

export const viewsManager = Role.builder()
  .id('views-manager-id')
  .name('Views Manager')
  .description('Allows reading and writing all views and extended searches (built-in)')
  .permissions(Immutable.Set(['view:edit', 'view:read']))
  .readOnly(true)
  .build();

export const reader = Role.builder()
  .id('reader-id')
  .name('Reader')
  .description('Grants basic permissions for every Graylog user (built-in)')
  .permissions(Immutable.Set(['clusterconfigentry:read', 'indexercluster:read']))
  .readOnly(true)
  .build();

export const reportCreator = Role.builder()
  .id('report-creator-id')
  .name('Report Creator')
  .description('Allows creation of Reports (built-in)')
  .permissions(Immutable.Set(['report:create']))
  .readOnly(true)
  .build();

export const customRole = Role.builder()
  .id('custom-role-id')
  .name('Custom Role')
  .description('Custom role (not built-in)')
  .permissions(Immutable.Set(['archivelicense:read', 'archive:read']))
  .readOnly(false)
  .build();

export const rolesList = Immutable.List<Role>([manager, alertsManager, viewsManager, reader, reportCreator, customRole]);
