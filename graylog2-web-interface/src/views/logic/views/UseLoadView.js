// @flow strict
import { useEffect, useState } from 'react';

import View from './View';
import { processHooks } from './ViewLoader';

import type { ViewHook } from '../hooks/ViewHook';
import usePluginEntities from '../usePluginEntities';

const useLoadView = (view: Promise<View>, query: { [string]: any }) => {
  const loadingViewHooks: Array<ViewHook> = usePluginEntities('views.hooks.loadingView');
  const executingViewHooks: Array<ViewHook> = usePluginEntities('views.hooks.executingView');

  const [loaded, setLoaded] = useState(false);
  const [hookComponent, setHookComponent] = useState(undefined);

  useEffect(() => {
    processHooks(
      view,
      loadingViewHooks,
      executingViewHooks,
      query,
      () => setLoaded(true),
    ).catch((e) => {
      if (e instanceof Error) {
        throw e;
      }

      setHookComponent(e);
    });
  }, [executingViewHooks, loadingViewHooks, view]);

  return [loaded, hookComponent];
};

export default useLoadView;
