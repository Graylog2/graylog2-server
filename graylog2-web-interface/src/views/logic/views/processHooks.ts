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
import type SearchExecutionState from 'views/logic/search/SearchExecutionState';

import type View from './View';

import type { ViewHook, ViewHookArguments } from '../hooks/ViewHook';

const checkReturnType = ((result) => {
  if (!result || !(result instanceof Array) || result.length < 2) {
    // eslint-disable-next-line no-console
    console.error('Return value supplied by processing hook is not array with two elements. It is: ', JSON.stringify(result, null, 2));
  }

  return result;
});

const _chainHooks = (hooks: Array<ViewHook>, args: ViewHookArguments) => hooks.reduce(
  (prev, cur: ViewHook) => prev.then(checkReturnType)
    .then(([newView, newExecutionState]) => cur({ ...args, view: newView, executionState: newExecutionState })),
  Promise.resolve([args.view, args.executionState] as const),
);

type Query = { [key: string]: any };
type OnSuccess = (view: View, executionState: SearchExecutionState) => void;

const _processViewHooks = (viewHooks: Array<ViewHook>, view: View, query: Query, executionState: SearchExecutionState, onSuccess: OnSuccess) => {
  let promise: Promise<readonly [View, SearchExecutionState]>;

  if (viewHooks.length > 0) {
    const retry: ViewHookArguments['retry'] = (args) => {
      const _view = args.view ?? view;
      const _executionState = args.executionState ?? executionState;

      return _processViewHooks(viewHooks, _view, query, _executionState, onSuccess);
    };

    promise = _chainHooks(viewHooks, { view, retry, query, executionState });
  } else {
    promise = Promise.resolve([view, executionState] as const);
  }

  return promise.then(checkReturnType).then(async ([newView, newExecutionState]) => {
    await onSuccess(newView, newExecutionState);

    return [newView, newExecutionState] as const;
  });
};

const processHooks = (
  promise: Promise<View>,
  executionState: SearchExecutionState,
  loadingViewHooks: Array<ViewHook> = [],
  executingViewHooks: Array<ViewHook> = [],
  query: Query = {},
  onSuccess: OnSuccess = () => {},
) => promise
  .then((view: View) => _processViewHooks(loadingViewHooks, view, query, executionState, onSuccess))
  .then(([newView, newExecutionState]) => _processViewHooks(executingViewHooks, newView, query, newExecutionState, onSuccess));

export default processHooks;
