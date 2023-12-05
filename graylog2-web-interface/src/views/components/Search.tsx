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
import { useCallback, useEffect, useContext, useMemo } from 'react';
import styled, { css } from 'styled-components';

import PageContentLayout from 'components/layout/PageContentLayout';
import { useStore } from 'stores/connect';
import Sidebar from 'views/components/sidebar/Sidebar';
import SearchResult from 'views/components/SearchResult';
import { SearchConfigActions, SearchConfigStore } from 'views/stores/SearchConfigStore';
import { StreamsActions } from 'views/stores/StreamsStore';
import HeaderElements from 'views/components/HeaderElements';
import QueryBarElements from 'views/components/QueryBarElements';
import WindowLeaveMessage from 'views/components/common/WindowLeaveMessage';
import IfDashboard from 'views/components/dashboard/IfDashboard';
import QueryBar from 'views/components/QueryBar';
import { FieldsOverview } from 'views/components/sidebar';
import DashboardSearchBar from 'views/components/DashboardSearchBar';
import SearchBar from 'views/components/SearchBar';
import IfSearch from 'views/components/search/IfSearch';
import IfInteractive from 'views/components/dashboard/IfInteractive';
import HighlightMessageInQuery from 'views/components/messagelist/HighlightMessageInQuery';
import { AdditionalContext } from 'views/logic/ActionContext';
import DefaultFieldTypesProvider from 'views/components/contexts/DefaultFieldTypesProvider';
import InteractiveContext from 'views/components/contexts/InteractiveContext';
import useSearchPageLayout from 'hooks/useSearchPageLayout';
import HighlightingRulesProvider from 'views/components/contexts/HighlightingRulesProvider';
import SearchPagePreferencesProvider from 'views/components/contexts/SearchPagePreferencesProvider';
import WidgetFocusProvider from 'views/components/contexts/WidgetFocusProvider';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import useCurrentUser from 'hooks/useCurrentUser';
import SynchronizeUrl from 'views/components/SynchronizeUrl';
import useView from 'views/hooks/useView';
import useAppDispatch from 'stores/useAppDispatch';
import { execute } from 'views/logic/slices/searchExecutionSlice';
import { selectCurrentQueryResults } from 'views/logic/slices/viewSelectors';
import useAppSelector from 'stores/useAppSelector';
import useParameters from 'views/hooks/useParameters';

import ExternalValueActionsProvider from './ExternalValueActionsProvider';

const GridContainer = styled.div<{ $interactive: boolean }>(({ $interactive }) => ($interactive ? css`
    display: flex;
    overflow: auto;
    height: 100%;

    > *:nth-child(2) {
      flex-grow: 1;
    }
` : css`
    flex: 1;
`));

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

const ConnectedSidebar = (props: Omit<React.ComponentProps<typeof Sidebar>, 'results'>) => {
  const results = useAppSelector(selectCurrentQueryResults);

  return <Sidebar results={results} {...props} />;
};

const ViewAdditionalContextProvider = ({ children }: { children: React.ReactNode }) => {
  const view = useView();
  const { searchesClusterConfig } = useStore(SearchConfigStore) ?? {};
  const { parameters, parameterBindings } = useParameters();
  const currentUser = useCurrentUser();
  const contextValue = useMemo(() => ({
    view,
    analysisDisabledFields: searchesClusterConfig?.analysis_disabled_fields,
    currentUser,
    parameters,
    parameterBindings,
  }), [currentUser, parameterBindings, parameters, searchesClusterConfig?.analysis_disabled_fields, view]);

  return (
    <AdditionalContext.Provider value={contextValue}>
      {children}
    </AdditionalContext.Provider>
  );
};

ViewAdditionalContextProvider.displayName = 'ViewAdditionalContextProvider';

const Search = () => {
  const dispatch = useAppDispatch();
  const refreshSearch = useCallback(() => dispatch(execute()), [dispatch]);
  const { sidebar: { isShown: showSidebar }, searchAreaContainer, infoBar } = useSearchPageLayout();
  const InfoBar = infoBar?.component;
  const SearchAreaContainer = searchAreaContainer?.component;

  useEffect(() => {
    refreshSearch();
  }, [refreshSearch]);

  useEffect(() => {
    SearchConfigActions.refresh();

    StreamsActions.refresh();
  }, []);

  return (
    <>
      <SynchronizeUrl />
      <ExternalValueActionsProvider>
        <WidgetFocusProvider>
          <WidgetFocusContext.Consumer>
            {({
              focusedWidget: { focusing: focusingWidget, editing: editingWidget } = {
                focusing: false,
                editing: false,
              },
            }) => (
              <>
                <IfInteractive>
                  <IfDashboard>
                    <WindowLeaveMessage />
                  </IfDashboard>
                </IfInteractive>
                <InteractiveContext.Consumer>
                  {(interactive) => (
                    <SearchPagePreferencesProvider>
                      <DefaultFieldTypesProvider>
                        <ViewAdditionalContextProvider>
                          <HighlightingRulesProvider>
                            <GridContainer id="main-row" $interactive={interactive}>
                              <IfInteractive>
                                {showSidebar && (
                                <ConnectedSidebar>
                                  <FieldsOverview />
                                </ConnectedSidebar>
                                )}
                              </IfInteractive>
                              <SearchArea as={SearchAreaContainer}>
                                <IfInteractive>
                                  <HeaderElements />
                                  {InfoBar && <InfoBar />}
                                  <IfDashboard>
                                    {!editingWidget && <DashboardSearchBar />}
                                  </IfDashboard>
                                  <IfSearch>
                                    <SearchBar />
                                  </IfSearch>

                                  <QueryBarElements />

                                  <IfDashboard>
                                    {!focusingWidget && <QueryBar />}
                                  </IfDashboard>
                                </IfInteractive>
                                <HighlightMessageInQuery>
                                  <SearchResult />
                                </HighlightMessageInQuery>
                              </SearchArea>
                            </GridContainer>
                          </HighlightingRulesProvider>
                        </ViewAdditionalContextProvider>
                      </DefaultFieldTypesProvider>
                    </SearchPagePreferencesProvider>
                  )}
                </InteractiveContext.Consumer>
              </>
            )}
          </WidgetFocusContext.Consumer>
        </WidgetFocusProvider>
      </ExternalValueActionsProvider>
    </>
  );
};

export default Search;
