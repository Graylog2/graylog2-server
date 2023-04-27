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
import React from 'react';
import { renderHook } from 'wrappedTestingLibrary/hooks';

import {
  ltParamJSON, urlConfigWithAgg,
} from 'fixtures/createEventDefinitionFromValue';
import useLocalStorageConfigData from 'views/logic/valueactions/createEventDefinition/hooks/useLocalStorageConfigData';

const wrapper = ({ children }) => (
  <div>
    {children}
  </div>
);

describe('useUrlConfigData', () => {
  it('concat all query values correct and return rest', async () => {
    const { result, waitFor } = renderHook(() => useLocalStorageConfigData({
      mappedData: {
        searchWithinMs: 300000,
        searchFilterQuery: '(http_method:GET)',
        queryWithReplacedParams: 'http_method:GET',
        searchFromValue: 'action:show',
        aggField: 'action',
        aggFunction: 'count',
        aggValue: 400,
        columnGroupBy: ['action', 'http_method'],
        rowGroupBy: ['action'],
        streams: ['streamId-1', 'streamId-2'],
        lutParameters: [ltParamJSON],
      },
      checked: {
        searchWithinMs: true,
        searchFilterQuery: true,
        queryWithReplacedParams: true,
        searchFromValue: true,
        aggCondition: true,
        columnGroupBy: true,
        rowGroupBy: true,
        streams: true,
        lutParameters: true,
      },
    }), { wrapper });
    await waitFor(() => !!result.current);

    expect(result.current).toEqual(urlConfigWithAgg);
  });

  it('ignore non-selected values', async () => {
    const { result, waitFor } = renderHook(() => useLocalStorageConfigData({
      mappedData: {
        searchWithinMs: 300000,
        searchFilterQuery: '(http_method:GET)',
        queryWithReplacedParams: 'http_method:GET',
        searchFromValue: 'action:show',
        aggField: 'action',
        aggFunction: 'count',
        aggValue: 400,
        columnGroupBy: ['action', 'http_method'],
        rowGroupBy: ['action'],
        streams: ['streamId-1', 'streamId-2'],
        lutParameters: [ltParamJSON],
      },
      checked: {
        searchWithinMs: true,
        searchFilterQuery: true,
        aggCondition: true,
        columnGroupBy: true,
        streams: true,
      },
    }), { wrapper });
    await waitFor(() => !!result.current);

    expect(result.current).toEqual({
      agg_field: 'action',
      agg_function: 'count',
      agg_value: 400,
      group_by: [
        'action',
        'http_method',
      ],
      query: '(http_method:GET)',
      search_within_ms: 300000,
      streams: [
        'streamId-1',
        'streamId-2',
      ],
      type: 'aggregation-v1',
    });
  });
});
