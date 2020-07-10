import * as Immutable from 'immutable';

import User from 'logic/users/User';

export const alice = User.builder()
  .id('alice-id')
  .username('alice')
  .fullName('Alice Schwarzer')
  .email('alice@example.org')
  .permissions(Immutable.List('streams:read:*', 'dashboard:create:*'))
  .roles(Immutable.List('admin', 'reader'))
  .readOnly(false)
  .external(false)
  .sessionTimeoutMs(0)
  .sessionActive(true)
  .clientAddress('127.0.0.1')
  .build();

export const bob = User.builder()
  .id('bob-id')
  .username('bob')
  .fullName('Bob Bobson')
  .email('bob@example.org')
  .permissions(Immutable.List('streams:read:*', 'dashboard:create:*'))
  .roles(Immutable.List('admin', 'reader'))
  .readOnly(false)
  .external(true)
  .sessionTimeoutMs(0)
  .sessionActive(false)
  .clientAddress()
  .build();

export const admin = User.builder()
  .id('admin-id')
  .username('admin')
  .fullName('Administrator')
  .email('admin@example.org')
  .permissions(Immutable.List('streams:read:*', 'dashboard:create:*'))
  .roles(Immutable.List('admin', 'reader'))
  .readOnly(false)
  .external(true)
  .sessionTimeoutMs(0)
  .sessionActive(true)
  .clientAddress('192.168.0.1')
  .build();

export const userList = Immutable.List([admin, bob, alice]);
