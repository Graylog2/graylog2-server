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
import { useCallback, useEffect, useContext, useState, useMemo } from 'react';
import * as Immutable from 'immutable';
import styled, { css } from 'styled-components';

import PageContentLayout from 'components/layout/PageContentLayout';
import connect, { useStore } from 'stores/connect';
import Sidebar from 'views/components/sidebar/Sidebar';
import WithSearchStatus from 'views/components/WithSearchStatus';
import SearchResult from 'views/components/SearchResult';
import { SearchStore, SearchActions } from 'views/stores/SearchStore';
import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';
import { SearchConfigActions, SearchConfigStore } from 'views/stores/SearchConfigStore';
import { SearchMetadataActions } from 'views/stores/SearchMetadataStore';
import { StreamsActions } from 'views/stores/StreamsStore';
import { ViewActions, ViewStore } from 'views/stores/ViewStore';
import HeaderElements from 'views/components/HeaderElements';
import QueryBarElements from 'views/components/QueryBarElements';
import WindowLeaveMessage from 'views/components/common/WindowLeaveMessage';
import IfDashboard from 'views/components/dashboard/IfDashboard';
import QueryBar from 'views/components/QueryBar';
import { FieldsOverview } from 'views/components/sidebar';
import DashboardSearchBar from 'views/components/DashboardSearchBar';
import SearchBar from 'views/components/SearchBar';
import CurrentViewTypeProvider from 'views/components/views/CurrentViewTypeProvider';
import IfSearch from 'views/components/search/IfSearch';
import IfInteractive from 'views/components/dashboard/IfInteractive';
import HighlightMessageInQuery from 'views/components/messagelist/HighlightMessageInQuery';
import { ViewMetadataStore } from 'views/stores/ViewMetadataStore';
import { AdditionalContext } from 'views/logic/ActionContext';
import DefaultFieldTypesProvider from 'views/components/contexts/DefaultFieldTypesProvider';
import InteractiveContext from 'views/components/contexts/InteractiveContext';
import HighlightingRulesProvider from 'views/components/contexts/HighlightingRulesProvider';
import SearchPageLayoutProvider from 'views/components/contexts/SearchPageLayoutProvider';
import WidgetFocusProvider from 'views/components/contexts/WidgetFocusProvider';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import type SearchExecutionState from 'views/logic/search/SearchExecutionState';
import type { RefluxActions } from 'stores/StoreTypes';
import CurrentUserContext from 'contexts/CurrentUserContext';
import SynchronizeUrl from 'views/components/SynchronizeUrl';

const GridContainer = styled.div<{ interactive: boolean }>(({ interactive }) => {
  return interactive ? css`
    display: flex;
    overflow: auto;
    height: 100%;

    > *:nth-child(2) {
      flex-grow: 1;
    }
  ` : css`
    flex: 1;
  `;
});

const SearchArea = styled(PageContentLayout)(() => {
  const { focusedWidget } = useContext(WidgetFocusContext);

  return css`
    ${focusedWidget?.id && css`
      .page-content-grid {
        display: flex;
        flex-direction: column;
        height: 100%;
        width: 100%;

        /* overflow auto is required to display the message table widget height correctly */
        overflow: ${focusedWidget?.id ? 'auto' : 'visible'};
      }
    `}
  `;
});

const ConnectedSidebar = connect(
  Sidebar,
  { viewMetadata: ViewMetadataStore, searches: SearchStore },
  ({ viewMetadata, searches }) => ({
    viewMetadata,
    queryId: viewMetadata.activeQuery,
    results: searches?.result?.forId(viewMetadata.activeQuery),
  }),
);

const _refreshSearch = (executionState: SearchExecutionState) => {
  const { view } = ViewStore.getInitialState();

  return SearchMetadataActions.parseSearch(view.search).then(() => {
    return SearchActions.execute(executionState).then(() => {});
  });
};

const SearchBarWithStatus = WithSearchStatus(SearchBar);
const DashboardSearchBarWithStatus = WithSearchStatus(DashboardSearchBar);

const ViewAdditionalContextProvider = ({ children }: { children: React.ReactNode }) => {
  const { view } = useStore(ViewStore);
  const { searchesClusterConfig } = useStore(SearchConfigStore) ?? {};
  const currentUser = useContext(CurrentUserContext);
  const contextValue = useMemo(() => ({
    view,
    analysisDisabledFields: searchesClusterConfig?.analysis_disabled_fields,
    currentUser,
  }), [currentUser, searchesClusterConfig?.analysis_disabled_fields, view]);

  return (
    <AdditionalContext.Provider value={contextValue}>
      {children}
    </AdditionalContext.Provider>
  );
};

ViewAdditionalContextProvider.displayName = 'ViewAdditionalContextProvider';

const useRefreshSearchOn = (_actions: Array<RefluxActions<any>>, refresh: () => Promise<any>) => {
  useEffect(() => {
    let storeListenersUnsubscribes = Immutable.List<() => void>();

    refresh().finally(() => {
      storeListenersUnsubscribes = storeListenersUnsubscribes
        .push(SearchActions.refresh.listen(refresh))
        .push(ViewActions.search.completed.listen(refresh));
    });

    // Returning cleanup function used when unmounting
    return () => { storeListenersUnsubscribes.forEach((unsubscribeFunc) => unsubscribeFunc()); };
  }, [refresh]);
};

const Search = () => {
  const [hasErrors] = useState(false);
  const refreshSearch = useCallback(
    () => _refreshSearch(SearchExecutionStateStore.getInitialState()),
    [],
  );

  useRefreshSearchOn([SearchActions.refresh, ViewActions.search], refreshSearch);

  useEffect(() => {
    SearchConfigActions.refresh();

    StreamsActions.refresh();
  }, []);

  return (
    <>
      <SynchronizeUrl />
      <WidgetFocusProvider>
        <WidgetFocusContext.Consumer>
          {({
            focusedWidget: { focusing: focusingWidget, editing: editingWidget } = {
              focusing: false,
              editing: false,
            },
          }) => (
            <CurrentViewTypeProvider>
              <IfInteractive>
                <IfDashboard>
                  <WindowLeaveMessage />
                </IfDashboard>
              </IfInteractive>
              <InteractiveContext.Consumer>
                {(interactive) => (
                  <SearchPageLayoutProvider>
                    <DefaultFieldTypesProvider>
                      <ViewAdditionalContextProvider>
                        <HighlightingRulesProvider>
                          <GridContainer id="main-row" interactive={interactive}>
                            <IfInteractive>
                              <ConnectedSidebar>
                                <FieldsOverview />
                              </ConnectedSidebar>
                            </IfInteractive>
                            <SearchArea>
                              <IfInteractive>
                                <HeaderElements />
                                <IfDashboard>
                                  {!editingWidget && <DashboardSearchBarWithStatus />}
                                </IfDashboard>
                                <IfSearch>
                                  <SearchBarWithStatus />
                                </IfSearch>

                                <QueryBarElements />

                                <IfDashboard>
                                  {!focusingWidget && <QueryBar />}
                                </IfDashboard>
                              </IfInteractive>
                              <HighlightMessageInQuery>
                                <SearchResult hasErrors={hasErrors} />
                              </HighlightMessageInQuery>
                            </SearchArea>
                          </GridContainer>
                        </HighlightingRulesProvider>
                      </ViewAdditionalContextProvider>
                    </DefaultFieldTypesProvider>
                  </SearchPageLayoutProvider>
                )}
              </InteractiveContext.Consumer>
            </CurrentViewTypeProvider>
          )}
        </WidgetFocusContext.Consumer>
      </WidgetFocusProvider>
    </>
  );
};

export default Search;
