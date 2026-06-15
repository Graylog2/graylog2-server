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
import { renderHook, act } from 'wrappedTestingLibrary/hooks';
import { waitFor } from 'wrappedTestingLibrary';
import { useQueryClient } from '@tanstack/react-query';

import asMock from 'helpers/mocking/AsMock';
import fetch from 'logic/rest/FetchProvider';
import { deleteRule, RULES_QUERY_KEY } from 'components/rules/hooks/useRules';
import type { RuleType } from 'components/rules/hooks/useRules';

import useDebugMetricsConfig from './useDebugMetricsConfig';

jest.mock('logic/rest/FetchProvider', () => jest.fn());
jest.mock('util/UserNotification', () => ({ error: jest.fn(), success: jest.fn() }));

describe('useDebugMetricsConfig', () => {
  afterEach(() => {
    jest.resetAllMocks();
  });

  // Regression test for the original RulesStore bug (#26302): deleting a rule must not
  // clobber the metrics config. The Reflux store triggered a partial state without
  // `metricsConfig`; the react-query migration keeps the metrics config in its own
  // query key (`rule-metrics-config`), so invalidating the rules query on delete leaves
  // it untouched. This guards against a future regression that re-couples the two caches.
  it('keeps metricsEnabled when a rule is deleted', async () => {
    asMock(fetch).mockImplementation((method) =>
      method === 'GET' ? Promise.resolve({ metrics_enabled: true }) : Promise.resolve(undefined),
    );

    const { result } = renderHook(() => ({
      config: useDebugMetricsConfig(),
      queryClient: useQueryClient(),
    }));

    await waitFor(() => expect(result.current.config.metricsEnabled).toBe(true));

    await act(async () => {
      await deleteRule({ id: 'rule-1', title: 'test rule' } as RuleType);
      await result.current.queryClient.invalidateQueries({ queryKey: RULES_QUERY_KEY });
    });

    expect(result.current.config.metricsEnabled).toBe(true);
  });
});
