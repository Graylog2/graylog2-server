// @flow strict
import Routes from 'routing/Routes';
import history from 'util/History';

import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import type { ViewHook, ViewHookArguments } from '../hooks/ViewHook';
import View from './View';
import ViewDeserializer from './ViewDeserializer';

const _chainHooks = (hooks: Array<ViewHook>, args: ViewHookArguments) => {
  return hooks.reduce((prev, cur: ViewHook) => prev.then(() => cur(args)), Promise.resolve());
};

type Query = { [string]: any };
type OnSuccess = () => void;
type OnError = () => void;

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

const ViewLoader = (viewId: string,
  loadingViewHooks: Array<ViewHook> = [],
  executingViewHooks: Array<ViewHook> = [],
  query: Query = {},
  onSuccess: OnSuccess = () => {},
  onError: OnError = () => {}) => {
  return ViewManagementActions.get(viewId)
    .then(ViewDeserializer, (e) => {
      if (e.status === 404) {
        history.replace(Routes.NOTFOUND);
      } else {
        throw e;
      }
      return View.create();
    })
    .then((view) => {
      return _processViewHooks(loadingViewHooks, view, query, onSuccess);
    })
    .then((view) => {
      return _processViewHooks(executingViewHooks, view, query, onSuccess);
    })
    .catch(onError);
};

export type ViewLoaderFn = typeof ViewLoader;

export default ViewLoader;
