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
import { renderHook, waitFor } from 'wrappedTestingLibrary/hooks';

import useEventDefinitionConfigFromLocalStorage from 'components/event-definitions/hooks/useEventDefinitionConfigFromLocalStorage';
import asMock from 'helpers/mocking/AsMock';
import useQuery from 'routing/useQuery';
import {
  urlConfigWithAgg,
  urlConfigWithFunctionAgg,
  urlConfigWithoutAgg,
} from 'fixtures/createEventDefinitionFromValue';
import Store from 'logic/local-storage/Store';

jest.mock('routing/useQuery');

describe('useEventDefinitionConfigFromLocalStorage', () => {
  beforeEach(() => {
    asMock(useQuery).mockReturnValue({ 'session-id': 'session-id' });
  });

  afterEach(() => {
    Store.delete('session-id');
  });

  it('return data with conditions part when function, field and value exist', async () => {
    Store.set('session-id', urlConfigWithAgg);

    const { result } = renderHook(() => useEventDefinitionConfigFromLocalStorage());

    await waitFor(() =>
      expect(result.current).toEqual({
        configFromLocalStorage: {
          conditions: {
            expression: {
              left: {
                expr: 'number-ref',
                ref: 'count-action',
              },
              right: {
                expr: 'number',
                value: 400,
              },
            },
          },
          group_by: ['action', 'action', 'http_method'],
          query: '(http_method:GET) AND ((http_method:GET)) AND (action:show)',
          query_parameters: [
            {
              data_type: 'any',
              default_value: 'GET',
              description: '',
              key: 'lt',
              lookup_table: 'http_method',
              name: 'newParameter',
              optional: false,
              title: 'lt',
              type: 'lut-parameter-v1',
            },
          ],
          search_within_ms: 300000,
          series: [
            {
              field: 'action',
              type: 'count',
              id: 'count-action',
            },
          ],
          streams: ['streamId-1', 'streamId-2'],
          type: 'aggregation-v1',
        },
        hasLocalStorageConfig: true,
      }),
    );
  });

  it('return data with conditions part when only function and value exist', async () => {
    Store.set('session-id', urlConfigWithFunctionAgg);

    const { result } = renderHook(() => useEventDefinitionConfigFromLocalStorage());

    await waitFor(() =>
      expect(result.current).toEqual({
        configFromLocalStorage: {
          conditions: {
            expression: {
              left: {
                expr: 'number-ref',
                ref: 'count-',
              },
              right: {
                expr: 'number',
                value: 400,
              },
            },
          },
          group_by: ['action', 'action', 'http_method'],
          query: '(http_method:GET) AND ((http_method:GET)) AND (action:show)',
          query_parameters: [
            {
              data_type: 'any',
              default_value: 'GET',
              description: '',
              key: 'lt',
              lookup_table: 'http_method',
              name: 'newParameter',
              optional: false,
              title: 'lt',
              type: 'lut-parameter-v1',
            },
          ],
          search_within_ms: 300000,
          series: [
            {
              field: null,
              type: 'count',
              id: 'count-',
            },
          ],
          streams: ['streamId-1', 'streamId-2'],
          type: 'aggregation-v1',
        },
        hasLocalStorageConfig: true,
      }),
    );
  });

  it('return data without conditions part when function not exist', async () => {
    Store.set('session-id', urlConfigWithoutAgg);
    const { result } = renderHook(() => useEventDefinitionConfigFromLocalStorage());

    await waitFor(() =>
      expect(result.current).toEqual({
        configFromLocalStorage: {
          group_by: [],
          query: '(http_method:GET) AND ((http_method:GET)) AND (action:show)',
          query_parameters: [
            {
              data_type: 'any',
              default_value: 'GET',
              description: '',
              key: 'lt',
              lookup_table: 'http_method',
              name: 'newParameter',
              optional: false,
              title: 'lt',
              type: 'lut-parameter-v1',
            },
          ],
          search_within_ms: 300000,
          streams: ['streamId-1', 'streamId-2'],
          type: 'aggregation-v1',
        },
        hasLocalStorageConfig: true,
      }),
    );
  });

  it('returns the details, schedule and comparison when they are given, also for a threshold of 0', async () => {
    Store.set('session-id', {
      type: 'aggregation-v1',
      title: 'Any failed login',
      description: 'Failed logins',
      priority: 3,
      query: 'action:login_failed',
      search_within_ms: 600000,
      execute_every_ms: 60000,
      agg_function: 'count',
      agg_operator: '>',
      agg_value: 0,
    });

    const { result } = renderHook(() => useEventDefinitionConfigFromLocalStorage());

    await waitFor(() => expect(result.current.hasLocalStorageConfig).toBe(true));

    expect(result.current.detailsFromLocalStorage).toEqual({
      title: 'Any failed login',
      description: 'Failed logins',
      priority: 3,
    });
    expect(result.current.configFromLocalStorage).toEqual(
      expect.objectContaining({
        execute_every_ms: 60000,
        conditions: {
          expression: {
            expr: '>',
            left: { expr: 'number-ref', ref: 'count-' },
            right: { expr: 'number', value: 0 },
          },
        },
        series: [{ id: 'count-', type: 'count', field: null }],
      }),
    );
  });

  it('return hasUrlConfig when no url config data', async () => {
    const { result } = renderHook(() => useEventDefinitionConfigFromLocalStorage());

    await waitFor(() =>
      expect(result.current).toEqual({
        configFromLocalStorage: undefined,
        hasLocalStorageConfig: false,
      }),
    );
  });
});
