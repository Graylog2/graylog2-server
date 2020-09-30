// @flow strict
import { useEffect, useState } from 'react';

import type { ViewLoaderFn } from 'views/logic/views/ViewLoader';
import type { ViewHook } from 'views/logic/hooks/ViewHook';
import usePluginEntities from 'views/logic/usePluginEntities';

const useViewLoader = (viewId: string, query: { [string]: any }, viewLoader: ViewLoaderFn) => {
  const loadingViewHooks: Array<ViewHook> = usePluginEntities('views.hooks.loadingView');
  const executingViewHooks: Array<ViewHook> = usePluginEntities('views.hooks.executingView');

  const [loaded, setLoaded] = useState(false);
  const [hookComponent, setHookComponent] = useState(undefined);

  useEffect(() => {
    setLoaded(false);
    setHookComponent(undefined);

    viewLoader(
      viewId,
      loadingViewHooks,
      executingViewHooks,
      query,
      () => {
        setHookComponent(undefined);
      },
      (e) => {
        if (e instanceof Error) {
          throw e;
        }

        setHookComponent(e);
      },
    ).then((results) => {
      setLoaded(true);

      return results;
    }).catch((e) => e);
  }, [executingViewHooks, loadingViewHooks, viewId, viewLoader]);

  return [loaded, hookComponent];
};

export default useViewLoader;
