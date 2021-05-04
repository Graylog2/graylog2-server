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
import * as Immutable from 'immutable';
import { readerPermissions } from 'fixtures/permissions';

import User from 'logic/users/User';
import type { UserJSON } from 'logic/users/User';

export const viewsManager: UserJSON = {
  email: '',
  external: false,
  full_name: 'Betty Holberton',
  first_name: 'Betty',
  last_name: 'Holberton',
  id: 'user-id-1',
  last_activity: '2020-01-01T10:40:05.376+0000',
  permissions: ['dashboards:edit:view-id', 'view:edit:view-id'],
  grn_permissions: ['entity:own:grn::::dashboard:view-id', 'entity:own:grn::::view:view-id', 'entity:own:grn::::search:some-id'],
  preferences: { updateUnfocussed: false, enableSmartSearch: true, themeMode: 'teint' },
  read_only: true,
  roles: ['Views Manager'],
  session_active: true,
  session_timeout_ms: 28800000,
  timezone: 'UTC',
  username: 'betty',
  client_address: '127.0.0.1',
  account_status: 'enabled',
};

export const admin: UserJSON = {
  client_address: '127.0.0.1',
  first_name: '',
  last_name: '',
  email: '',
  external: false,
  full_name: 'Alonzo Church',
  id: 'user-id-2',
  last_activity: '2020-01-01T10:40:05.376+0000',
  permissions: ['*'],
  preferences: { updateUnfocussed: false, enableSmartSearch: true, themeMode: 'teint' },
  read_only: true,
  roles: ['Admin'],
  session_active: false,
  session_timeout_ms: 28800000,
  timezone: 'UTC',
  username: 'alonzo',
  account_status: 'enabled',
};

export const alice = User.builder()
  .id('alice-id')
  .username('alice')
  .fullName('Alice Schwarzer')
  .firstName('Alice')
  .lastName('Schwarzer')
  .email('alice@example.org')
  .permissions(Immutable.List(readerPermissions('alice')))
  .preferences({ updateUnfocussed: false, enableSmartSearch: true, themeMode: 'noir' })
  .roles(Immutable.Set(['Reader']))
  .readOnly(false)
  .external(false)
  .sessionActive(true)
  .sessionTimeoutMs(10000000000)
  .clientAddress('127.0.0.1')
  .accountStatus('enabled')
  .build();

export const bob = User.builder()
  .id('bob-id')
  .username('bob')
  .fullName('Bob Bobson')
  .firstName('Bob')
  .lastName('Bobson')
  .email('bob@example.org')
  .permissions(Immutable.List(readerPermissions('bob')))
  .preferences({ updateUnfocussed: false, enableSmartSearch: true, themeMode: 'teint' })
  .roles(Immutable.Set(['Reader']))
  .readOnly(false)
  .external(true)
  .sessionActive(false)
  .sessionTimeoutMs(10000000000)
  .clientAddress('172.0.0.1')
  .accountStatus('enabled')
  .build();

export const adminUser = User.builder()
  .id('admin-id')
  .username('admin')
  .fullName('Administrator')
  .firstName('Administrator')
  .lastName('')
  .email('admin@example.org')
  .permissions(Immutable.List(['*']))
  .roles(Immutable.Set(['Admin', 'Reader']))
  .readOnly(false)
  .external(true)
  .sessionActive(true)
  .sessionTimeoutMs(10000000000)
  .clientAddress('192.168.0.1')
  .accountStatus('enabled')
  .build();

export const userList = Immutable.List<User>([adminUser, bob, alice]);
