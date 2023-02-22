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

import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import Search from 'views/components/Search';
import { loadNewView as defaultLoadNewView, loadView as defaultLoadView } from 'views/logic/views/Actions';
import IfUserHasAccessToAnyStream from 'views/components/IfUserHasAccessToAnyStream';
import DashboardPageContextProvider from 'views/components/contexts/DashboardPageContextProvider';
import { DocumentTitle, Spinner } from 'components/common';
import type View from 'views/logic/views/View';
import useLoadView from 'views/hooks/useLoadView';
import useProcessHooksForView from 'views/logic/views/UseProcessHooksForView';
import useQuery from 'routing/useQuery';
import PluggableStoreProvider from 'components/PluggableStoreProvider';
import useViewTitle from 'views/hooks/useViewTitle';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';

type Props = React.PropsWithChildren<{
  isNew: boolean,
  view: Promise<View>,
  loadNewView?: () => unknown,
  loadView?: (viewId: string) => unknown,
  executionState?: SearchExecutionState,
}>;

const SearchPageTitle = ({ children }: { children: React.ReactNode }) => {
  const title = useViewTitle();

  return (
    <DocumentTitle title={title}>
      {children}
    </DocumentTitle>
  );
};

const SearchPage = ({ children, isNew, view: viewPromise, loadNewView = defaultLoadNewView, loadView = defaultLoadView, executionState: initialExecutionState }: Props) => {
  const query = useQuery();
  const initialQuery = query?.page as string;
  useLoadView(viewPromise, isNew);
  const result = useProcessHooksForView(viewPromise, initialExecutionState, query);

  if (result.status === 'loading') {
    return <Spinner />;
  }

  if (result.status === 'interrupted') {
    return result.component;
  }

  const { view, executionState } = result;

  return view
    ? (
      <PluggableStoreProvider view={view} executionState={executionState} isNew={isNew} initialQuery={initialQuery}>
        <SearchPageTitle>
          <DashboardPageContextProvider>
            <NewViewLoaderContext.Provider value={loadNewView}>
              <ViewLoaderContext.Provider value={loadView}>
                {children}
                <IfUserHasAccessToAnyStream>
                  <Search />
                </IfUserHasAccessToAnyStream>
              </ViewLoaderContext.Provider>
            </NewViewLoaderContext.Provider>
          </DashboardPageContextProvider>
        </SearchPageTitle>
      </PluggableStoreProvider>
    )
    : <Spinner />;
};

SearchPage.defaultProps = {
  loadNewView: defaultLoadNewView,
  loadView: defaultLoadView,
  executionState: SearchExecutionState.empty(),
};

export default React.memo(SearchPage);
