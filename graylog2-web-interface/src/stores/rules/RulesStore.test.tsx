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

import asMock from 'helpers/mocking/AsMock';
import fetch from 'logic/rest/FetchProvider';
import { useStore } from 'stores/connect';
import { RulesActions, RulesStore } from 'stores/rules/RulesStore';

jest.mock('logic/rest/FetchProvider', () => jest.fn());
jest.mock('util/UserNotification', () => ({ error: jest.fn(), success: jest.fn() }));

describe('RulesStore', () => {
  afterEach(() => {
    jest.resetAllMocks();
  });

  it('notifies subscribers with a new state when a fetched rule is added to existing rules', async () => {
    const ruleA = { id: 'rule-a', title: 'Rule A' };
    const ruleB = { id: 'rule-b', title: 'Rule B' };

    const { result } = renderHook(() => useStore(RulesStore));

    asMock(fetch).mockReturnValueOnce(Promise.resolve(ruleA));

    await act(async () => {
      await RulesActions.get('rule-a');
    });

    expect(result.current.rules).toEqual([ruleA]);

    const stateAfterFirstGet = result.current;

    asMock(fetch).mockReturnValueOnce(Promise.resolve(ruleB));

    await act(async () => {
      await RulesActions.get('rule-b');
    });

    // Subscribers must receive a new state snapshot, otherwise components
    // relying on state identity changes (e.g. RuleDetailsPage) never re-render.
    expect(result.current).not.toBe(stateAfterFirstGet);
    expect(result.current.rules).toEqual([ruleA, ruleB]);
  });
});
