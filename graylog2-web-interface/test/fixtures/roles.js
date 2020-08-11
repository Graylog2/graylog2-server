// @flow strict
import * as Immutable from 'immutable';

import Role from 'logic/roles/Role';

export const managerRole = Role.builder()
  .id('manager-role-id')
  .name('Manager')
  .description('Role description')
  .permissions(Immutable.Set(['dashboards:read']))
  .readOnly(true)
  .build();

export const rolesList = Immutable.List<Role>([managerRole]);
