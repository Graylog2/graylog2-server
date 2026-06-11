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
import mergePluginBindings from 'util/mergePluginBindings';

describe('mergePluginBindings', () => {
  it('concatenates array-valued keys from multiple bindings', () => {
    const merged = mergePluginBindings({ routes: ['a'] }, { routes: ['b'] }, { routes: ['c'] });

    expect(merged.routes).toEqual(['a', 'b', 'c']);
  });

  it('keeps array entries separate instead of merging them by index', () => {
    const baseNav = { description: 'Base', children: [{ description: 'base-child' }] };
    const injectedNav = { description: 'Injected', children: [{ description: 'injected-child' }] };

    const merged = mergePluginBindings({ pageNavigation: [baseNav] }, { pageNavigation: [injectedNav] });

    expect(merged.pageNavigation).toEqual([baseNav, injectedNav]);
  });

  it('preserves keys that only exist in one of the bindings', () => {
    const merged = mergePluginBindings({ routes: ['a'] }, { navigation: ['b'] });

    expect(merged).toEqual({ routes: ['a'], navigation: ['b'] });
  });

  it('deep-merges object-valued keys', () => {
    const merged = mergePluginBindings(
      { pages: { search: { component: 'SearchA' } } },
      { pages: { other: { component: 'OtherB' } } },
    );

    expect(merged.pages).toEqual({ search: { component: 'SearchA' }, other: { component: 'OtherB' } });
  });

  it('does not mutate any of the source bindings', () => {
    const first = { routes: ['a'] };
    const second = { routes: ['b'] };

    mergePluginBindings(first, second);

    expect(first).toEqual({ routes: ['a'] });
    expect(second).toEqual({ routes: ['b'] });
  });

  it('returns an empty object when called without bindings', () => {
    expect(mergePluginBindings()).toEqual({});
  });

  it('returns an equivalent object when given a single binding', () => {
    const merged = mergePluginBindings({ routes: ['a'], navigation: ['b'] });

    expect(merged).toEqual({ routes: ['a'], navigation: ['b'] });
  });
});
