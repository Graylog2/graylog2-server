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

import type { AppDispatch } from 'stores/useAppDispatch';

import type View from './View';

import type { ViewHook, ViewHookArguments } from '../hooks/ViewHook';

const _chainHooks = (hooks: Array<ViewHook>, args: ViewHookArguments) => {
  return hooks.reduce((prev, cur: ViewHook) => prev.then((newView) => cur({ ...args, view: newView })), Promise.resolve(args.view));
};

type Query = { [key: string]: any };
type OnSuccess = (view: View) => void;

const _processViewHooks = (viewHooks: Array<ViewHook>, view: View, query: Query, onSuccess: OnSuccess, dispatch: AppDispatch) => {
  let promise;

  if (viewHooks.length > 0) {
    const retry = () => _processViewHooks(viewHooks, view, query, onSuccess, dispatch);

    promise = _chainHooks(viewHooks, { view, retry, query, dispatch });
  } else {
    promise = Promise.resolve(view);
  }

  return promise.then(async (newView) => {
    await onSuccess(newView);

    return newView;
  });
};

const processHooks = (
  dispatch: AppDispatch,
  promise: Promise<View>,
  loadingViewHooks: Array<ViewHook> = [],
  executingViewHooks: Array<ViewHook> = [],
  query: Query = {},
  onSuccess: OnSuccess = () => {},
) => {
  return promise
    .then((view: View) => _processViewHooks(loadingViewHooks, view, query, onSuccess, dispatch))
    .then((view: View) => _processViewHooks(executingViewHooks, view, query, onSuccess, dispatch));
};

export default processHooks;
