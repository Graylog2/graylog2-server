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
import View from 'views/logic/views/View';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import type { ViewHook, ViewHookArguments } from 'views/logic/hooks/ViewHook';

import processHooks from './processHooks';

describe('processHooks', () => {
  const view = View.create();
  const executionState = SearchExecutionState.empty();

  it('passes user timezone to loading and executing view hooks', async () => {
    const loadingHook = jest.fn<ReturnType<ViewHook>, [ViewHookArguments]>(async (args) => [args.view, args.executionState] as const);
    const executingHook = jest.fn<ReturnType<ViewHook>, [ViewHookArguments]>(async (args) => [args.view, args.executionState] as const);

    await processHooks(
      Promise.resolve(view),
      executionState,
      [loadingHook],
      [executingHook],
      {},
      () => {},
      'America/New_York',
    );

    expect(loadingHook).toHaveBeenCalledWith(expect.objectContaining({ userTimezone: 'America/New_York' }));
    expect(executingHook).toHaveBeenCalledWith(expect.objectContaining({ userTimezone: 'America/New_York' }));
  });

  it('passes user timezone to view hooks on retry', async () => {
    const hook = jest.fn<ReturnType<ViewHook>, [ViewHookArguments]>(async (args) =>
      hook.mock.calls.length === 1
        ? (args.retry({ view: args.view, executionState: args.executionState }) as ReturnType<ViewHook>)
        : ([args.view, args.executionState] as const),
    );

    await processHooks(Promise.resolve(view), executionState, [hook], [], {}, () => {}, 'America/New_York');

    expect(hook).toHaveBeenCalledTimes(2);
    expect(hook.mock.calls[1][0].userTimezone).toBe('America/New_York');
  });
});
