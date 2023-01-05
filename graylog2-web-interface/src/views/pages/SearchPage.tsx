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
import { useSelector } from 'react-redux';

import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import Search from 'views/components/Search';
import { loadNewView as defaultLoadNewView, loadView as defaultLoadView } from 'views/logic/views/Actions';
import IfUserHasAccessToAnyStream from 'views/components/IfUserHasAccessToAnyStream';
import DashboardPageContextProvider from 'views/components/contexts/DashboardPageContextProvider';
import { DocumentTitle, Spinner } from 'components/common';
import type { RootState } from 'views/types';
import type View from 'views/logic/views/View';
import useLoadView from 'views/hooks/useLoadView';
import useProcessHooksForView from 'views/logic/views/UseProcessHooksForView';
import useQuery from 'routing/useQuery';
import PluggableStoreProvider from 'components/PluggableStoreProvider';

type Props = {
  isNew: boolean,
  view: Promise<View>,
  loadNewView?: () => unknown,
  loadView?: (viewId: string) => unknown,
};

const SearchPageTitle = ({ children }: { children: React.ReactNode }) => {
  const title = useSelector((state: RootState) => state.view.view?.title ?? `Unsaved ${state.view.view?.type}`);

  return (
    <DocumentTitle title={title}>
      {children}
    </DocumentTitle>
  );
};

const SearchPage = ({ isNew, view: viewPromise, loadNewView = defaultLoadNewView, loadView = defaultLoadView }: Props) => {
  const query = useQuery();
  useLoadView(viewPromise, query?.page as string, isNew);
  const [view, HookComponent] = useProcessHooksForView(viewPromise, query);

  if (HookComponent) {
    return HookComponent;
  }

  return view
    ? (
      <PluggableStoreProvider view={view} isNew={isNew}>
        <SearchPageTitle>
          <DashboardPageContextProvider>
            <NewViewLoaderContext.Provider value={loadNewView}>
              <ViewLoaderContext.Provider value={loadView}>
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
};

export default React.memo(SearchPage);
