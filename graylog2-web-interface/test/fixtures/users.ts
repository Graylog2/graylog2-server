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

export const alice = User.builder()
  .id('alice-id')
  .username('alice')
  .fullName('Alice Schwarzer')
  .firstName('Alice')
  .lastName('Schwarzer')
  .email('alice@example.org')
  .permissions(Immutable.List(readerPermissions('alice')))
  .grnPermissions(Immutable.List())
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
  .grnPermissions(Immutable.List())
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
  .grnPermissions(Immutable.List())
  .roles(Immutable.Set(['Admin', 'Reader']))
  .readOnly(false)
  .external(true)
  .sessionActive(true)
  .sessionTimeoutMs(10000000000)
  .clientAddress('192.168.0.1')
  .accountStatus('enabled')
  .build();

export const userList = Immutable.List<User>([adminUser, bob, alice]);
