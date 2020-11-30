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
// @flow strict
import ErrorsActions from 'actions/errors/ErrorsActions';
import { createFromFetchError } from 'logic/errors/ReportedErrors';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';

import View from './View';
import ViewDeserializer from './ViewDeserializer';

import type { ViewHook, ViewHookArguments } from '../hooks/ViewHook';

const _chainHooks = (hooks: Array<ViewHook>, args: ViewHookArguments) => {
  return hooks.reduce((prev, cur: ViewHook) => prev.then(() => cur(args)), Promise.resolve());
};

type Query = { [key: string]: any };
type OnSuccess = () => void;
type OnError = (e: Error) => void;

const _processViewHooks = (viewHooks: Array<ViewHook>, view: View, query: Query, onSuccess: OnSuccess) => {
  let promise;

  if (viewHooks.length > 0) {
    const retry = () => _processViewHooks(viewHooks, view, query, onSuccess);

    promise = _chainHooks(viewHooks, { view, retry, query });
  } else {
    promise = Promise.resolve(true);
  }

  return promise.then(() => view).then(onSuccess).then(() => view);
};

export const processHooks = (
  promise: Promise<View>,
  loadingViewHooks: Array<ViewHook> = [],
  executingViewHooks: Array<ViewHook> = [],
  query: Query = {},
  onSuccess: OnSuccess = () => {},
) => {
  return promise
    .then((view: View) => {
      return _processViewHooks(loadingViewHooks, view, query, onSuccess);
    })
    .then((view: View) => {
      return _processViewHooks(executingViewHooks, view, query, onSuccess);
    });
};

const ViewLoader = (viewId: string,
  loadingViewHooks: Array<ViewHook> = [],
  executingViewHooks: Array<ViewHook> = [],
  query: Query = {},
  onSuccess: OnSuccess = () => {},
  onError: OnError = () => {}) => {
  const promise = ViewManagementActions.get(viewId)
    .then(ViewDeserializer, (error) => {
      if (error.status === 404) {
        ErrorsActions.report(createFromFetchError(error));
      } else {
        throw error;
      }

      return View.create();
    });

  return processHooks(promise, loadingViewHooks, executingViewHooks, query, onSuccess)
    .catch(onError);
};

export type ViewLoaderFn = typeof ViewLoader;
export default ViewLoader;
