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
import { useEffect, useState } from 'react';
import * as React from 'react';

import ErrorPage from 'components/errors/ErrorPage';

import type View from './View';
import { processHooks } from './ViewLoader';

import usePluginEntities from '../usePluginEntities';

const LoadViewError = ({ error }: { error: Error }) => (
  <ErrorPage title="Something went wrong"
             description={<p>An unknown error has occurred. Please have a look at the following message and the graylog server log for more information.</p>}>
    <pre>
      {error?.message}
    </pre>
  </ErrorPage>
);

const useLoadView = (view: Promise<View>, query: { [key: string]: any }) => {
  const loadingViewHooks = usePluginEntities('views.hooks.loadingView');
  const executingViewHooks = usePluginEntities('views.hooks.executingView');

  const [loaded, setLoaded] = useState(false);
  const [hookComponent, setHookComponent] = useState(undefined);

  useEffect(() => {
    processHooks(
      view,
      loadingViewHooks,
      executingViewHooks,
      query,
      () => {
        setLoaded(true);
        setHookComponent(undefined);
      },
    ).catch((e) => {
      if (e instanceof Error) {
        setHookComponent(<LoadViewError error={e} />);

        return;
      }

      setHookComponent(e);
    });
  },
  // eslint-disable-next-line react-hooks/exhaustive-deps
  [executingViewHooks, loadingViewHooks, view]);

  return [loaded, hookComponent];
};

export default useLoadView;
