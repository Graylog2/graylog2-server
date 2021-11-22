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

import normalizeSearchURLQueryParams from './NormalizeSearchURLQueryParams';

describe('NormalizeSearchURLQueryParams', () => {
  it('should normalize relative time range with only a start', async () => {
    const result = normalizeSearchURLQueryParams({ rangetype: 'relative', relative: '600' });

    expect(result).toEqual({
      queryString: undefined,
      streamsFilter: null,
      timeRange: { type: 'relative', range: 600 },
    });
  });

  it('should normalize relative time range with only a start (from)', async () => {
    const result = normalizeSearchURLQueryParams({ rangetype: 'relative', from: '600' });

    expect(result).toEqual({
      queryString: undefined,
      streamsFilter: null,
      timeRange: { type: 'relative', from: 600 },
    });
  });

  it('should normalize relative time range with start and end', async () => {
    const result = normalizeSearchURLQueryParams({ rangetype: 'relative', from: '600', to: '300' });

    expect(result).toEqual({
      queryString: undefined,
      streamsFilter: null,
      timeRange: { type: 'relative', from: 600, to: 300 },
    });
  });

  it('should normalize absolute time range', async () => {
    const result = normalizeSearchURLQueryParams({
      rangetype: 'absolute',
      from: '2020-01-01T10:00:00.850Z',
      to: '2020-01-02T10:00:00.000Z',
    });

    expect(result).toEqual({
      queryString: undefined,
      streamsFilter: null,
      timeRange: { type: 'absolute', from: '2020-01-01T10:00:00.850Z', to: '2020-01-02T10:00:00.000Z' },
    });
  });

  it('should normalize keyword time range', async () => {
    const result = normalizeSearchURLQueryParams({ rangetype: 'keyword', keyword: 'yesterday' });

    expect(result).toEqual({
      queryString: undefined,
      streamsFilter: null,
      timeRange: { type: 'keyword', keyword: 'yesterday' },
    });
  });

  it('should normalize search query', async () => {
    const result = normalizeSearchURLQueryParams({ q: 'http_method:GET' });

    expect(result).toEqual({
      queryString: {
        query_string: 'http_method:GET',
        type: 'elasticsearch',
      },
      streamsFilter: null,
      timeRange: undefined,
    });
  });

  it('should normalize streams filter', async () => {
    const result = normalizeSearchURLQueryParams({ streams: 'stream-id-1,stream-id-2' });

    expect(result).toEqual({
      queryString: undefined,
      streamsFilter: Immutable.Map({
        type: 'or',
        filters: Immutable.List([
          Immutable.Map({
            type: 'stream',
            id: 'stream-id-1',
          }),
          Immutable.Map({
            type: 'stream',
            id: 'stream-id-2',
          }),
        ]),
      }),
      timeRange: undefined,
    });
  });
});
