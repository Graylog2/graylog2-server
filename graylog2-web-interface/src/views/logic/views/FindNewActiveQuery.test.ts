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

import FindNewActiveQuery from 'views/logic/views/FindNewActiveQuery';

const emptyList = Immutable.List<string>();

describe('FindNewActiveQuery', () => {
  it('does not break when there are no queries left', () => {
    expect(FindNewActiveQuery(emptyList, 'deadbeef', emptyList)).toBeUndefined();
    expect(FindNewActiveQuery(Immutable.List(['foo']), 'deadbeef', Immutable.List(['foo']))).toBeUndefined();
  });

  it('returns current query when it is still present', () => {
    expect(FindNewActiveQuery(Immutable.List(['foo', 'bar', 'baz']), 'foo', Immutable.List(['bar']))).toEqual('foo');
    expect(FindNewActiveQuery(Immutable.List(['foo', 'bar', 'baz']), 'foo', Immutable.List(['bar', 'baz']))).toEqual('foo');
  });

  it('returns next query when current query is removed', () => {
    expect(FindNewActiveQuery(Immutable.List(['foo', 'bar', 'baz']), 'bar', Immutable.List(['bar']))).toEqual('foo');
    expect(FindNewActiveQuery(Immutable.List(['foo', 'bar', 'baz']), 'foo', Immutable.List(['foo', 'baz']))).toEqual('bar');
    expect(FindNewActiveQuery(Immutable.List(['foo', 'bar', 'baz']), 'baz', Immutable.List(['baz']))).toEqual('bar');
    expect(FindNewActiveQuery(Immutable.List(['bar', 'baz']), 'foo', Immutable.List(['foo']))).toEqual('bar');
  });
});
