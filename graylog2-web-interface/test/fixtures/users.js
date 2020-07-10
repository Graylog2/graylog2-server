// @flow strict
import type { UserJSON as User } from 'stores/users/UsersStore';

/* eslint-disable import/prefer-default-export */

export const viewsManager: User = {
  client_address: '127.0.0.1',
  email: '',
  external: false,
  full_name: 'Betty Holberton',
  id: 'user-id-1',
  last_activity: '2020-01-01T10:40:05.376+0000',
  permissions: ['dashboards:edit:view-id', 'view:edit:view-id'],
  read_only: true,
  roles: ['Views Manager'],
  session_active: true,
  session_timeout_ms: 28800000,
  timezone: 'UTC',
  username: 'betty',
};

export const admin: User = {
  client_address: '127.0.0.1',
  email: '',
  external: false,
  full_name: 'Alonzo Church',
  id: 'user-id-2',
  last_activity: '2020-01-01T10:40:05.376+0000',
  permissions: ['*'],
  read_only: true,
  roles: ['Admin'],
  session_active: false,
  session_timeout_ms: 28800000,
  timezone: 'UTC',
  username: 'alonzo',
};
