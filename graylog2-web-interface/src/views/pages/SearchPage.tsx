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
import { useStore } from 'stores/connect';
import { DocumentTitle } from 'components/common';
import viewTitle from 'views/logic/views/ViewTitle';
import { ViewStore } from 'views/stores/ViewStore';
import type { LayoutState } from 'views/components/contexts/SearchPageLayoutContext';
import { SearchPageLayoutProvider } from 'views/components/contexts/SearchPageLayoutContext';

type Props = {
  loadNewView?: () => unknown,
  loadView?: (string) => unknown,
  providerOverrides?: LayoutState,
};

const SearchPageTitle = ({ children }: { children: React.ReactNode }) => {
  const title = useStore(ViewStore, ({ view }) => viewTitle(view?.title, view?.type));

  return (
    <DocumentTitle title={title}>
      {children}
    </DocumentTitle>
  );
};

const SearchPage = ({ loadNewView = defaultLoadNewView, loadView = defaultLoadView, providerOverrides = undefined }: Props) => (
  <SearchPageTitle>
    <DashboardPageContextProvider>
      <NewViewLoaderContext.Provider value={loadNewView}>
        <ViewLoaderContext.Provider value={loadView}>
          <IfUserHasAccessToAnyStream>
            <SearchPageLayoutProvider providerOverrides={providerOverrides}>
              <Search />
            </SearchPageLayoutProvider>
          </IfUserHasAccessToAnyStream>
        </ViewLoaderContext.Provider>
      </NewViewLoaderContext.Provider>
    </DashboardPageContextProvider>
  </SearchPageTitle>
);

SearchPage.defaultProps = {
  loadNewView: defaultLoadNewView,
  loadView: defaultLoadView,
  providerOverrides: undefined,
};

export default React.memo(SearchPage);
