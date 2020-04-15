// @flow strict
import type { User } from 'stores/users/UsersStore';

/* eslint-disable import/prefer-default-export */

export const viewsManager: User = {
  email: '',
  external: false,
  full_name: 'Betty Holberton',
  id: 'user-id-1',
  permissions: ['dashboards:edit:view-id', 'view:edit:view-id'],
  read_only: true,
  roles: ['Views Manager'],
  session_timeout_ms: 28800000,
  timezone: 'UTC',
  username: 'betty',
};

export const admin: User = {
  email: '',
  external: false,
  full_name: 'Alonzo Church',
  id: 'user-id-2',
  permissions: ['*'],
  read_only: true,
  roles: ['Admin'],
  session_timeout_ms: 28800000,
  timezone: 'UTC',
  username: 'alonzo',
};
