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
import * as React from 'react';
import { useEffect, useState } from 'react';

import { ViewLoaderFn } from 'views/logic/views/ViewLoader';
import { ViewHook } from 'views/logic/hooks/ViewHook';
import usePluginEntities from 'views/logic/usePluginEntities';

const useViewLoader = (viewId: string, query: { [key: string]: any }, viewLoader: ViewLoaderFn): [boolean, React.ReactElement | undefined] => {
  const loadingViewHooks: Array<ViewHook> = usePluginEntities('views.hooks.loadingView');
  const executingViewHooks: Array<ViewHook> = usePluginEntities('views.hooks.executingView');

  const [loaded, setLoaded] = useState(false);
  const [hookComponent, setHookComponent] = useState(undefined);

  useEffect(() => {
    setLoaded(false);
    setHookComponent(undefined);

    viewLoader(viewId, loadingViewHooks, executingViewHooks, query, () => {
      setHookComponent(undefined);
    }, (e) => {
      if (e instanceof Error) {
        throw e;
      }

      setHookComponent(e);
    }).then((results) => {
      setLoaded(true);

      return results;
    }).catch((e) => e);
  }, // eslint-disable-next-line react-hooks/exhaustive-deps
  [executingViewHooks, loadingViewHooks, viewId, viewLoader]);

  return [loaded, hookComponent];
};

export default useViewLoader;
