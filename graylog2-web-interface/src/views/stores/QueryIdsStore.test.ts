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
import { asMock, MockStore } from 'helpers/mocking';

import { ViewStore } from 'views/stores/ViewStore';
import Query from 'views/logic/queries/Query';
import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';

import { QueryIdsStore } from './QueryIdsStore';

jest.mock('./ViewStore', () => ({
  ViewStore: MockStore(),
}));

const createViewStoreState = (queryIds: Array<string>) => ({
  view: View.builder()
    .search(Search.builder()
      .queries(Immutable.OrderedSet(queryIds.map((queryId) => Query.builder().id(queryId).build())))
      .build())
    .build(),
  activeQuery: 'foo',
  dirty: false,
  isNew: false,
});

describe('QueryIdsStore', () => {
  it('tracks `ViewStore` updates', async () => {
    const cb = asMock(ViewStore.listen).mock.calls[0][0] as Parameters<typeof ViewStore.listen>[0];

    const result = await new Promise((resolve) => {
      QueryIdsStore.listen((queryIds) => resolve(queryIds));

      cb(createViewStoreState(['foo']));
    });

    expect(result).toEqual(Immutable.OrderedSet(['foo']));
  });

  it('keeps order of query ids', async () => {
    const cb = asMock(ViewStore.listen).mock.calls[0][0] as Parameters<typeof ViewStore.listen>[0];

    const result = await new Promise((resolve) => {
      QueryIdsStore.listen((queryIds) => resolve(queryIds));

      cb(createViewStoreState(['foo', 'bar', 'baz']));
    });

    expect(result).toEqual(Immutable.OrderedSet(['foo', 'bar', 'baz']));
  });
});
