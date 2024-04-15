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
import * as React from 'react';
import * as Immutable from 'immutable';
import { renderHook } from 'wrappedTestingLibrary/hooks';

import Query from 'views/logic/queries/Query';
import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';
import useQueryIds from 'views/hooks/useQueryIds';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';

const createView = (queryIds: Array<string>) => View.builder()
  .search(Search.builder()
    .queries(Immutable.OrderedSet(queryIds.map((queryId) => Query.builder().id(queryId).build())))
    .build())
  .build();

const Wrapper = ({ children, queryIds }: React.PropsWithChildren<{ queryIds: Array<string> }>) => (
  <TestStoreProvider view={createView(queryIds)}>
    {children}
  </TestStoreProvider>
);
const createWrapper = (queryIds: Array<string>) => ({ children }: React.PropsWithChildren<{}>) => (
  <Wrapper queryIds={queryIds}>
    {children}
  </Wrapper>
);

describe('useQueryIds', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  useViewsPlugin();

  it('tracks query updates', async () => {
    const { result } = renderHook(() => useQueryIds(), { wrapper: createWrapper(['foo']) });

    expect(result.current).toEqual(Immutable.OrderedSet(['foo']));
  });

  it('keeps order of query ids', async () => {
    const { result } = renderHook(() => useQueryIds(), { wrapper: createWrapper(['foo', 'bar', 'baz']) });

    expect(result.current).toEqual(Immutable.OrderedSet(['foo', 'bar', 'baz']));
  });
});
