// @flow strict
import * as Immutable from 'immutable';

import UserOverview from 'logic/users/UserOverview';

export const alice = UserOverview.builder()
  .id('alice-id')
  .username('alice')
  .fullName('Alice Schwarzer')
  .email('alice@example.org')
  .roles(Immutable.Set(['Reader']))
  .readOnly(false)
  .external(false)
  .sessionActive(true)
  .clientAddress('127.0.0.1')
  .build();

export const bob = UserOverview.builder()
  .id('bob-id')
  .username('bob')
  .fullName('Bob Bobson')
  .email('bob@example.org')
  .roles(Immutable.Set(['Reader']))
  .readOnly(false)
  .external(true)
  .sessionActive(false)
  .clientAddress('172.0.0.1')
  .build();

export const charlie = UserOverview.builder()
  .id('charlie-id')
  .username('charlie')
  .fullName('Charlie Root')
  .email('charlie@example.org')
  .roles(Immutable.Set(['Reader']))
  .readOnly(false)
  .external(true)
  .sessionActive(false)
  .clientAddress('172.0.0.1')
  .build();

export const admin = UserOverview.builder()
  .id('admin-id')
  .username('admin')
  .fullName('Administrator')
  .email('admin@example.org')
  .roles(Immutable.Set(['Admin']))
  .readOnly(false)
  .external(true)
  .sessionActive(true)
  .clientAddress('192.168.0.1')
  .build();

export const userList = Immutable.List<UserOverview>([admin, bob, alice]);

export const paginatedUsers = {
  list: userList,
  pagination: {
    page: 1,
    perPage: 10,
    query: '',
    count: userList.size,
    total: userList.size,
  },
  adminUser: undefined,
};
