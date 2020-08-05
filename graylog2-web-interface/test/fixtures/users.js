// @flow strict
import type { UserJSON } from 'logic/users/User';

/* eslint-disable import/prefer-default-export */

export const viewsManager: UserJSON = {
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
  session_active: false,
  client_address: '127.0.0.1',
  last_activity: '2020-01-01T10:40:05.376+0000',
};

export const admin: UserJSON = {
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
  session_active: false,
  client_address: '127.0.0.1',
  last_activity: '2020-01-01T10:40:05.376+0000',
};
