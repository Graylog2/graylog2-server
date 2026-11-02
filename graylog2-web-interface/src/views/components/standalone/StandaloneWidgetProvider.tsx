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
import { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';

import { Spinner } from 'components/common';
import ViewsStoreProvider from 'views/stores/ViewsStoreProvider';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import AutoRefreshContext from 'views/components/contexts/AutoRefreshContext';
import WidgetContext from 'views/components/contexts/WidgetContext';
import WidgetFieldTypesContextProvider from 'views/components/contexts/WidgetFieldTypesContextProvider';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import useWidget from 'views/hooks/useWidget';
import { executeActiveQuery } from 'views/logic/slices/viewSlice';
import type Widget from 'views/logic/widgets/Widget';

import createStandaloneView, { STANDALONE_QUERY_ID } from './createStandaloneView';

const noop = () => {};
const autoRefreshDisabled = {
  animationId: '',
  refreshConfig: { enabled: false, interval: 0 },
  restartAutoRefresh: noop,
  startAutoRefresh: noop,
  stopAutoRefresh: noop,
};

const ExecuteOnMount = () => {
  const dispatch = useViewsDispatch();

  useEffect(() => {
    dispatch(executeActiveQuery());
  }, [dispatch]);

  return null;
};

const WidgetFromStore = ({ widgetId, children = undefined }: React.PropsWithChildren<{ widgetId: string }>) => {
  const widget = useWidget(widgetId);

  return (
    <WidgetContext.Provider value={widget}>
      <WidgetFieldTypesContextProvider>{children}</WidgetFieldTypesContextProvider>
    </WidgetContext.Provider>
  );
};

type Props = React.PropsWithChildren<{
  widget: Widget;
  title: string;
}>;

/**
 * Renders a single widget outside of a search or dashboard, in its own in-memory views store.
 * The store holds a temporary view that is never saved; only the search used for executing it is created.
 */
const StandaloneWidgetProvider = ({ widget, title, children = undefined }: Props) => {
  const { data: view } = useQuery({
    queryKey: ['views', 'standalone-widget', widget, title],
    queryFn: () => createStandaloneView(widget, title),
    staleTime: Infinity,
    gcTime: 0,
  });

  if (!view) {
    return <Spinner text="Loading widget..." />;
  }

  return (
    <ViewsStoreProvider
      view={view}
      isNew={false}
      initialQuery={STANDALONE_QUERY_ID}
      executionState={SearchExecutionState.empty()}>
      <AutoRefreshContext.Provider value={autoRefreshDisabled}>
        <ExecuteOnMount />
        <WidgetFromStore widgetId={widget.id}>{children}</WidgetFromStore>
      </AutoRefreshContext.Provider>
    </ViewsStoreProvider>
  );
};

export default StandaloneWidgetProvider;
