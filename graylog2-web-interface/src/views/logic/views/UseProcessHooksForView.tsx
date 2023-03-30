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
import usePluginEntities from 'hooks/usePluginEntities';
import type SearchExecutionState from 'views/logic/search/SearchExecutionState';

import type View from './View';
import processHooks from './processHooks';

const LoadViewError = ({ error }: { error: Error }) => {
  useEffect(() => {
    // eslint-disable-next-line no-console
    console.log(error);
  }, [error]);

  return (
    <ErrorPage title="Something went wrong"
               description={(
                 <p>An unknown error has occurred. Please have a look at the following message and the
                   graylog server log for more information.
                 </p>
)}>
      <pre>
        {error?.message}
      </pre>
    </ErrorPage>
  );
};

type HookComponent = JSX.Element;

type Loading = { status: 'loading' };
type Loaded = { status: 'loaded', view: View, executionState: SearchExecutionState };
type Interrupted = { status: 'interrupted', component: HookComponent };
type ResultType = Loading | Loaded | Interrupted;

const useProcessHooksForView = (view: Promise<View>, executionState: SearchExecutionState, query: { [key: string]: any }): ResultType => {
  const loadingViewHooks = usePluginEntities('views.hooks.loadingView');
  const executingViewHooks = usePluginEntities('views.hooks.executingView');

  const [result, setResult] = useState<ResultType>({ status: 'loading' });

  useEffect(() => {
    processHooks(
      view,
      executionState,
      loadingViewHooks,
      executingViewHooks,
      query,
      (v, e) => {
        setResult({ status: 'loaded', view: v, executionState: e });
      },
    ).catch((e: Error | HookComponent) => {
      if (e instanceof Error) {
        // eslint-disable-next-line no-console
        console.error(e);
      }

      const component = e instanceof Error
        ? <LoadViewError error={e} />
        : e;

      setResult({ status: 'interrupted', component });
    });
  },
  // eslint-disable-next-line react-hooks/exhaustive-deps
  [executingViewHooks, loadingViewHooks, view]);

  return result;
};

export default useProcessHooksForView;
