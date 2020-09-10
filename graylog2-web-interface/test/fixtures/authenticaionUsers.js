// @flow strict
import * as Immutable from 'immutable';

import AuthenticationUser from 'logic/authentication/AuthenticationUser';

import { services } from './authentication';

export const alice = AuthenticationUser.builder()
  .id('alice-id')
  .username('alice')
  .fullName('Alice Schwarzer')
  .email('alice@example.org')
  .roles(Immutable.List(['Reader']))
  .readOnly(false)
  .external(false)
  .sessionActive(true)
  .clientAddress('127.0.0.1')
  .backendId(services.first().id)
  .backendGuid('backend-guid')
  .enabled(true)
  .build();

export const bob = AuthenticationUser.builder()
  .id('bob-id')
  .username('bob')
  .fullName('Bob Bobson')
  .email('bob@example.org')
  .roles(Immutable.List(['Reader']))
  .readOnly(false)
  .external(true)
  .sessionActive(false)
  .clientAddress('172.0.0.1')
  .backendId(services.first().id)
  .backendGuid('backend-guid')
  .enabled(true)
  .build();

export const admin = AuthenticationUser.builder()
  .id('admin-id')
  .username('admin')
  .fullName('Administrator')
  .email('admin@example.org')
  .roles(Immutable.List(['Admin']))
  .readOnly(false)
  .external(true)
  .sessionActive(true)
  .clientAddress('192.168.0.1')
  .backendId(services.first().id)
  .backendGuid('backend-guid')
  .enabled(false)
  .build();

export const userList = Immutable.List<AuthenticationUser>([admin, bob, alice]);
