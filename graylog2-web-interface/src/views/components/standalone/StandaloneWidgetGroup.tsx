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
import { useContext, useEffect, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import styled from 'styled-components';

import { Spinner } from 'components/common';
import ViewsStoreProvider from 'views/stores/ViewsStoreProvider';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import AutoRefreshContext from 'views/components/contexts/AutoRefreshContext';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import { executeActiveQuery } from 'views/logic/slices/viewSlice';
import type Widget from 'views/logic/widgets/Widget';
import SearchExecutorsContext from 'views/components/contexts/SearchExecutorsContext';
import InteractiveContext from 'views/components/contexts/InteractiveContext';
import type { InteractionMode } from 'views/components/contexts/InteractiveContext';
import ErrorWidget from 'views/components/widgets/ErrorWidget';
import type { SearchExecutors } from 'views/logic/slices/searchExecutionSlice';
import useIntersectionObserver from 'hooks/useIntersectionObserver';

import createStandaloneView, { STANDALONE_QUERY_ID } from './createStandaloneView';

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

// Takes the space the widgets will take, so the layout does not jump when they render.
const LazyPlaceholder = styled.div`
  flex: 1;
  width: 100%;
  height: 100%;
  min-height: 0;
`;

const RenderWhenNearViewport = ({ children }: React.PropsWithChildren) => {
  const placeholderRef = useRef<HTMLDivElement>(null);
  // Renders a bit before the widgets scroll into view, so they are usually ready by then.
  const isNearViewport = useIntersectionObserver(placeholderRef, { rootMargin: '200px' });
  // Keeps the widgets once rendered, so scrolling past them does not run their search again.
  const [wasNearViewport, setWasNearViewport] = useState(false);

  if (isNearViewport && !wasNearViewport) {
    setWasNearViewport(true);
  }

  return wasNearViewport ? children : <LazyPlaceholder ref={placeholderRef} />;
};

type GroupProps = React.PropsWithChildren<{
  widgets: Array<Widget>;
  titles: { [widgetId: string]: string };
  loadingPlaceholder: React.ReactNode;
}>;

const Group = ({ widgets, titles, loadingPlaceholder, children = undefined }: GroupProps) => {
  const { createSearch } = useContext(SearchExecutorsContext);
  const { data: view, error } = useQuery({
    queryKey: ['views', 'standalone-widgets', widgets, titles, createSearch],
    queryFn: () => createStandaloneView(widgets, titles, createSearch),
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
  /** Runs the search instead of the search page's executors, e.g. to show stored results. Must be stable across renders. */
  searchExecutors?: SearchExecutors;
  /** Shown while the group prepares its search, e.g. skeletons of its widgets. */
  loadingPlaceholder?: React.ReactNode;
  /**
   * Most field and value actions change the search or add widgets, which the page does not show,
   * so the widgets are read-only by default.
   */
  interactionMode?: InteractionMode;
  /**
   * Renders the widgets and runs their search only once the group comes close to the viewport, e.g. for long lists
   * of widgets. Its container needs a height until then.
   */
  lazy?: boolean;
}>;

/**
 * Runs one search for several widgets outside of a search or dashboard, in its own in-memory views store.
 * The store holds a temporary view that is never saved. Like on the search page, its search is stored to run it.
 * Render the widgets anywhere inside the group with `StandaloneWidgetCard` or `StandaloneWidgetContent`.
 */
const StandaloneWidgetGroup = ({
  widgets,
  titles = {},
  searchExecutors = undefined,
  loadingPlaceholder = <Spinner text="Loading widgets..." />,
  interactionMode = 'read-only',
  lazy = false,
  children = undefined,
}: Props) => {
  const content = (
    <Group widgets={widgets} titles={titles} loadingPlaceholder={loadingPlaceholder}>
      {children}
    </Group>
  );
  const group = lazy ? <RenderWhenNearViewport>{content}</RenderWhenNearViewport> : content;

  return (
    <InteractiveContext.Provider value={interactionMode}>
      {searchExecutors ? (
        <SearchExecutorsContext.Provider value={searchExecutors}>{group}</SearchExecutorsContext.Provider>
      ) : (
        group
      )}
    </InteractiveContext.Provider>
  );
};

export default StandaloneWidgetGroup;
