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
import { AdditionalContext } from 'views/logic/ActionContext';
import WidgetFieldTypesContextProvider from 'views/components/contexts/WidgetFieldTypesContextProvider';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import useWidget from 'views/hooks/useWidget';
import { executeActiveQuery } from 'views/logic/slices/viewSlice';
import type Widget from 'views/logic/widgets/Widget';
import SearchExecutorsContext from 'views/components/contexts/SearchExecutorsContext';
import InteractiveContext from 'views/components/contexts/InteractiveContext';
import type { InteractionMode } from 'views/components/contexts/InteractiveContext';
import ErrorWidget from 'views/components/widgets/ErrorWidget';
import type { SearchExecutors } from 'views/logic/slices/searchExecutionSlice';

import createStandaloneView, { STANDALONE_QUERY_ID } from './createStandaloneView';
import RenderWhenVisible from './RenderWhenVisible';

// Stable across renders: the views store is created again whenever its initial execution state changes, which
// runs the search again.
const EMPTY_EXECUTION_STATE = SearchExecutionState.empty();

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

/**
 * Provides a widget of the enclosing `StandaloneWidgetGroup` to its children, like the search page does for its widgets.
 */
export const WidgetFromGroup = ({ widgetId, children = undefined }: React.PropsWithChildren<{ widgetId: string }>) => {
  const widget = useWidget(widgetId);

  return (
    <WidgetContext.Provider value={widget}>
      {/* Like on the search page, field and value actions get the widget, e.g. for values of several groupings. */}
      <AdditionalContext.Provider value={{ widget }}>
        <WidgetFieldTypesContextProvider>{children}</WidgetFieldTypesContextProvider>
      </AdditionalContext.Provider>
    </WidgetContext.Provider>
  );
};

type GroupProps = React.PropsWithChildren<{
  widgets: Array<Widget>;
  titles: { [widgetId: string]: string };
  loadingPlaceholder: React.ReactNode;
}>;

const Group = ({ widgets, titles, loadingPlaceholder, children = undefined }: GroupProps) => {
  const { data: view, error } = useQuery({
    queryKey: ['views', 'standalone-widgets', widgets, titles],
    queryFn: () => createStandaloneView(widgets, titles),
    staleTime: Infinity,
    gcTime: 0,
  });

  if (error) {
    return (
      <ErrorWidget
        title="While preparing the search for these widgets, the following error occurred:"
        errors={[{ description: error.message }]}
      />
    );
  }

  if (!view) {
    return loadingPlaceholder;
  }

  return (
    <ViewsStoreProvider
      view={view}
      isNew={false}
      initialQuery={STANDALONE_QUERY_ID}
      executionState={EMPTY_EXECUTION_STATE}>
      <AutoRefreshContext.Provider value={autoRefreshDisabled}>
        <ExecuteOnMount />
        {children}
      </AutoRefreshContext.Provider>
    </ViewsStoreProvider>
  );
};

type Props = React.PropsWithChildren<{
  widgets: Array<Widget>;
  titles?: { [widgetId: string]: string };
  /**
   * Creates the views store and runs the search only once the group comes close to the viewport.
   * Its container needs a height until then.
   */
  lazy?: boolean;
  /** Runs the search instead of the search page's executors, e.g. to show stored results. Must be stable across renders. */
  searchExecutors?: SearchExecutors;
  /** Shown while the group prepares its search, e.g. skeletons of its widgets. */
  loadingPlaceholder?: React.ReactNode;
  /**
   * Most field and value actions change the search or add widgets, which the page does not show,
   * so the widgets are read-only by default.
   */
  interactionMode?: InteractionMode;
}>;

/**
 * Runs one search for several widgets outside of a search or dashboard, in its own in-memory views store.
 * The store holds a temporary view that is never saved. Like on the search page, its search is stored to run it.
 * Render the widgets anywhere inside the group with `GroupedWidget`.
 */
const StandaloneWidgetGroup = ({
  widgets,
  titles = {},
  lazy = false,
  searchExecutors = undefined,
  loadingPlaceholder = <Spinner text="Loading widgets..." />,
  interactionMode = 'read-only',
  children = undefined,
}: Props) => {
  const group = (
    <Group widgets={widgets} titles={titles} loadingPlaceholder={loadingPlaceholder}>
      {children}
    </Group>
  );
  const groupWithExecutors = searchExecutors ? (
    <SearchExecutorsContext.Provider value={searchExecutors}>{group}</SearchExecutorsContext.Provider>
  ) : (
    group
  );

  return (
    <InteractiveContext.Provider value={interactionMode}>
      {lazy ? <RenderWhenVisible>{groupWithExecutors}</RenderWhenVisible> : groupWithExecutors}
    </InteractiveContext.Provider>
  );
};

export default StandaloneWidgetGroup;
