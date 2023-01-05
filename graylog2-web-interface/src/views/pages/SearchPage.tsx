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
import { useDispatch, useSelector } from 'react-redux';

import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import NewViewLoaderContext from 'views/logic/NewViewLoaderContext';
import Search from 'views/components/Search';
import { loadNewView as defaultLoadNewView, loadView as defaultLoadView } from 'views/logic/views/Actions';
import IfUserHasAccessToAnyStream from 'views/components/IfUserHasAccessToAnyStream';
import DashboardPageContextProvider from 'views/components/contexts/DashboardPageContextProvider';
import { DocumentTitle, Spinner } from 'components/common';
import type { RootState } from 'views/types';
import { load } from 'views/logic/slices/viewSlice';
import type View from 'views/logic/views/View';
import useLoadView from 'views/hooks/useLoadView';
import useProcessHooksForView from 'views/logic/views/UseProcessHooksForView';
import useQuery from 'routing/useQuery';

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

const SearchPage = ({ isNew, view, loadNewView = defaultLoadNewView, loadView = defaultLoadView }: Props) => {
  const query = useQuery();
  useLoadView(view, query?.page as string, isNew);
  const [loaded, HookComponent] = useProcessHooksForView(view, query);
  const dispatch = useDispatch();

  useEffect(() => {
    view.then(load).then(dispatch);
  }, [dispatch, view]);

  if (HookComponent) {
    return HookComponent;
  }

  return (loaded)
    ? (
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
    )
    : <Spinner />;
};

SearchPage.defaultProps = {
  loadNewView: defaultLoadNewView,
  loadView: defaultLoadView,
};

export default React.memo(SearchPage);
