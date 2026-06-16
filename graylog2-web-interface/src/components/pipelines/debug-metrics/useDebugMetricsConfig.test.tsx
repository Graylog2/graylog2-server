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

import asMock from 'helpers/mocking/AsMock';
import fetch from 'logic/rest/FetchProvider';
import { RulesActions } from 'stores/rules/RulesStore';
import type { RuleType } from 'stores/rules/RulesStore';

import useDebugMetricsConfig from './useDebugMetricsConfig';

jest.mock('logic/rest/FetchProvider', () => jest.fn());
jest.mock('util/UserNotification', () => ({ error: jest.fn(), success: jest.fn() }));

describe('useDebugMetricsConfig', () => {
  afterEach(() => {
    jest.resetAllMocks();
  });

  it('keeps metricsEnabled when a rule is deleted', async () => {
    asMock(fetch).mockImplementation((method) =>
      method === 'GET' ? Promise.resolve({ metrics_enabled: true }) : Promise.resolve(undefined),
    );

    const { result } = renderHook(() => useDebugMetricsConfig());

    await waitFor(() => expect(result.current.metricsEnabled).toBe(true));

    await act(async () => {
      await RulesActions.delete({ id: 'rule-1', title: 'test rule' } as RuleType);
    });

    expect(result.current.metricsEnabled).toBe(true);
  });
});
