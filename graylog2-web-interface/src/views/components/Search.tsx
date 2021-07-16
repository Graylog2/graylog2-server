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
import { useCallback, useEffect, useContext, useState } from 'react';
import * as Immutable from 'immutable';
import styled, { css } from 'styled-components';

import PageContentLayout from 'components/layout/PageContentLayout';
import withLocation from 'routing/withLocation';
import type { Location } from 'routing/withLocation';
import connect from 'stores/connect';
import Sidebar from 'views/components/sidebar/Sidebar';
import WithSearchStatus from 'views/components/WithSearchStatus';
import SearchResult from 'views/components/SearchResult';
import type {
  SearchRefreshCondition,
  SearchRefreshConditionArguments,
} from 'views/logic/hooks/SearchRefreshCondition';
import { FieldTypesActions } from 'views/stores/FieldTypesStore';
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
import bindSearchParamsFromQuery from 'views/hooks/BindSearchParamsFromQuery';
import { useSyncWithQueryParameters } from 'views/hooks/SyncWithQueryParameters';
import { AdditionalContext } from 'views/logic/ActionContext';
import DefaultFieldTypesProvider from 'views/components/contexts/DefaultFieldTypesProvider';
import InteractiveContext from 'views/components/contexts/InteractiveContext';
import HighlightingRulesProvider from 'views/components/contexts/HighlightingRulesProvider';
import SearchPageLayoutProvider from 'views/components/contexts/SearchPageLayoutProvider';
import usePluginEntities from 'views/logic/usePluginEntities';
import WidgetFocusProvider from 'views/components/contexts/WidgetFocusProvider';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import { RefluxActions } from 'stores/StoreTypes';

const GridContainer = styled.div<{ interactive: boolean }>(({ interactive }) => {
  return interactive ? css`
    display: flex;
    overflow: auto;
    height: 100%;

    > *:nth-child(2) {
      flex-grow: 1;
    }
  ` : css`
    flex: 1
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
  { viewMetadata: ViewMetadataStore, searches: SearchStore, view: ViewStore },
  ({ viewMetadata, view, searches }) => ({
    viewMetadata,
    viewIsNew: view.isNew,
    queryId: viewMetadata.activeQuery,
    results: searches?.result?.forId(viewMetadata.activeQuery),
  }),
);

type Props = {
  location: Location,
};

const _searchRefreshConditionChain = (searchRefreshHooks: Array<SearchRefreshCondition>, state: SearchRefreshConditionArguments) => {
  if (!searchRefreshHooks || searchRefreshHooks.length === 0) {
    return true;
  }

  return searchRefreshHooks.every((condition: SearchRefreshCondition) => condition(state));
};

const _refreshIfNotUndeclared = (searchRefreshHooks: Array<SearchRefreshCondition>, executionState: SearchExecutionState, setHasErrors: (hasErrors: boolean) => void) => {
  const { view } = ViewStore.getInitialState();

  return SearchMetadataActions.parseSearch(view.search).then((searchMetadata) => {
    if (_searchRefreshConditionChain(searchRefreshHooks, { view, searchMetadata, executionState })) {
      FieldTypesActions.refresh();

      setHasErrors(false);

      return SearchActions.execute(executionState);
    }

    setHasErrors(true);

    return Promise.reject(searchMetadata);
  });
};

const SearchBarWithStatus = WithSearchStatus(SearchBar);
const DashboardSearchBarWithStatus = WithSearchStatus(DashboardSearchBar);

const ViewAdditionalContextProvider = connect(
  AdditionalContext.Provider,
  { view: ViewStore, configs: SearchConfigStore },
  ({ view, configs: { searchesClusterConfig } }) => ({ value: { view: view.view, analysisDisabledFields: searchesClusterConfig?.analysis_disabled_fields } } as { value: object}),
);

ViewAdditionalContextProvider.displayName = 'ViewAdditionalContextProvider';

const useRefreshSearchOn = (actions: Array<RefluxActions<any>>, refresh: () => Promise<any>) => {
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

const useBindSearchParamsFromQuery = (query: { [key: string]: unknown }) => {
  useEffect(() => {
    const { view } = ViewStore.getInitialState();

    bindSearchParamsFromQuery({ view, query, retry: () => Promise.resolve() });
  }, [query]);
};

const Search = ({ location }: Props) => {
  const { pathname, search } = location;
  const query = `${pathname}${search}`;
  const searchRefreshHooks = usePluginEntities('views.hooks.searchRefresh');
  const [hasErrors, setHasErrors] = useState(false);
  const refreshIfNotUndeclared = useCallback(
    () => _refreshIfNotUndeclared(searchRefreshHooks, SearchExecutionStateStore.getInitialState(), setHasErrors),
    [searchRefreshHooks],
  );

  useBindSearchParamsFromQuery(location.query);
  useSyncWithQueryParameters(query);

  useRefreshSearchOn([SearchActions.refresh, ViewActions.search], refreshIfNotUndeclared);

  useEffect(() => {
    SearchConfigActions.refresh();

    StreamsActions.refresh();
  }, []);

  return (
    <WidgetFocusProvider>
      <WidgetFocusContext.Consumer>
        {({ focusedWidget: { focusing: focusingWidget, editing: editingWidget } = { focusing: false, editing: false } }) => (
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
                                {!editingWidget && <DashboardSearchBarWithStatus onExecute={refreshIfNotUndeclared} />}
                              </IfDashboard>
                              <IfSearch>
                                <SearchBarWithStatus onExecute={refreshIfNotUndeclared} />
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
  );
};

export default withLocation(Search);
