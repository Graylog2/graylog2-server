// @flow strict
import * as Immutable from 'immutable';
import { readerPermissions } from 'fixtures/permissions';

import User from 'logic/users/User';
import type { UserJSON } from 'logic/users/User';

export const viewsManager: UserJSON = {
  email: '',
  external: false,
  full_name: 'Betty Holberton',
  id: 'user-id-1',
  last_activity: '2020-01-01T10:40:05.376+0000',
  permissions: ['dashboards:edit:view-id', 'view:edit:view-id'],
  grn_permissions: ['entity:own:grn::::dashboard:view-id', 'entity:own:grn::::view:view-id', 'entity:own:grn::::search:some-id'],
  read_only: true,
  roles: ['Views Manager'],
  session_active: true,
  session_timeout_ms: 28800000,
  timezone: 'UTC',
  username: 'betty',
  client_address: '127.0.0.1',
};

export const admin: UserJSON = {
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

export const alice = User.builder()
  .id('alice-id')
  .username('alice')
  .fullName('Alice Schwarzer')
  .email('alice@example.org')
  .permissions(Immutable.List(readerPermissions('alice')))
  .roles(Immutable.List(['Reader']))
  .readOnly(false)
  .external(false)
  .sessionActive(true)
  .sessionTimeoutMs(10000000000)
  .clientAddress('127.0.0.1')
  .build();

export const bob = User.builder()
  .id('bob-id')
  .username('bob')
  .fullName('Bob Bobson')
  .email('bob@example.org')
  .permissions(Immutable.List(readerPermissions('bob')))
  .roles(Immutable.List(['Reader']))
  .readOnly(false)
  .external(true)
  .sessionActive(false)
  .sessionTimeoutMs(10000000000)
  .clientAddress('172.0.0.1')
  .build();

export const adminUser = User.builder()
  .id('admin-id')
  .username('admin')
  .fullName('Administrator')
  .email('admin@example.org')
  .permissions(Immutable.List(['*']))
  .roles(Immutable.List(['Admin', 'Reader']))
  .readOnly(false)
  .external(true)
  .sessionActive(true)
  .sessionTimeoutMs(10000000000)
  .clientAddress('192.168.0.1')
  .build();

export const userList = Immutable.List<User>([adminUser, bob, alice]);
