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
