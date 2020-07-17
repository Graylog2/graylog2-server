// @flow strict
import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import styled, { css } from 'styled-components';
import type { StyledComponent } from 'styled-components';
import { withRouter } from 'react-router';

import connect from 'stores/connect';
import Footer from 'components/layout/Footer';
import AppContentGrid from 'components/layout/AppContentGrid';
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
import withPluginEntities from 'views/logic/withPluginEntities';
import { AdditionalContext } from 'views/logic/ActionContext';
import DefaultFieldTypesProvider from 'views/components/contexts/DefaultFieldTypesProvider';
import InteractiveContext from 'views/components/contexts/InteractiveContext';
import HighlightingRulesProvider from 'views/components/contexts/HighlightingRulesProvider';
import SearchPageLayoutProvider from 'views/components/contexts/SearchPageLayoutProvider';

const GridContainer: StyledComponent<{ interactive: boolean }, void, HTMLDivElement> = styled.div`
  ${({ interactive }) => (interactive ? css`
    height: calc(100vh - 50px);
    display: flex;
    > *:nth-child(2) {
      flex-grow: 1;
    }
  ` : '')}
`;

const SearchArea: StyledComponent<{}, void, *> = styled(AppContentGrid)`
  height: 100%;
  z-index: 1;
  overflow-y: auto;
`;

const ConnectedSidebar = connect(
  Sidebar,
  { viewMetadata: ViewMetadataStore, searches: SearchStore, view: ViewStore },
  (props) => ({
    ...props,
    viewIsNew: props.view.isNew,
    queryId: props.viewMetadata.activeQuery,
    results: props.searches && props.searches.result ? props.searches.result.forId(props.viewMetadata.activeQuery) : undefined,
  }),
);

type Props = {
  route: any,
  searchRefreshHooks: Array<SearchRefreshCondition>,
  router: {
    getCurrentLocation: () => ({ pathname: string, search: string }),
  },
  location?: {
    query: { [string]: string },
  },
};

const _searchRefreshConditionChain = (searchRefreshHooks, state: SearchRefreshConditionArguments) => {
  if (!searchRefreshHooks || searchRefreshHooks.length === 0) {
    return true;
  }

  return searchRefreshHooks.every((condition: SearchRefreshCondition) => condition(state));
};

const _refreshIfNotUndeclared = (searchRefreshHooks, executionState) => {
  const { view } = ViewStore.getInitialState();

  return SearchMetadataActions.parseSearch(view.search).then((searchMetadata) => {
    if (_searchRefreshConditionChain(searchRefreshHooks, { view, searchMetadata, executionState })) {
      FieldTypesActions.all();

      return SearchActions.execute(executionState);
    }

    return Promise.reject(searchMetadata);
  });
};

const SearchBarWithStatus = WithSearchStatus(SearchBar);
const DashboardSearchBarWithStatus = WithSearchStatus(DashboardSearchBar);

const ViewAdditionalContextProvider = connect(
  AdditionalContext.Provider,
  { view: ViewStore, configs: SearchConfigStore },
  ({ view, configs: { searchesClusterConfig } }) => ({ value: { view: view.view, analysisDisabledFields: searchesClusterConfig.analysis_disabled_fields } }),
);

const ExtendedSearchPage = ({ route, location = { query: {} }, router, searchRefreshHooks }: Props) => {
  const { pathname, search } = router.getCurrentLocation();
  const query = `${pathname}${search}`;
  const refreshIfNotUndeclared = () => _refreshIfNotUndeclared(searchRefreshHooks, SearchExecutionStateStore.getInitialState());

  useEffect(() => {
    const { view } = ViewStore.getInitialState();

    bindSearchParamsFromQuery({ view, query: location.query, retry: () => Promise.resolve() });
  }, [query]);

  useEffect(() => {
    SearchConfigActions.refresh();

    FieldTypesActions.all();

    StreamsActions.refresh();

    let storeListenersUnsubscribes = Immutable.List();

    refreshIfNotUndeclared().finally(() => {
      storeListenersUnsubscribes = storeListenersUnsubscribes
        .push(SearchActions.refresh.listen(refreshIfNotUndeclared))
        .push(ViewActions.search.completed.listen(refreshIfNotUndeclared));

      return null;
    });

    // Returning cleanup function used when unmounting
    return () => { storeListenersUnsubscribes.forEach((unsubscribeFunc) => unsubscribeFunc()); };
  }, []);

  useSyncWithQueryParameters(query);

  return (
    <CurrentViewTypeProvider>
      <IfInteractive>
        <IfDashboard>
          <WindowLeaveMessage route={route} />
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
                          <DashboardSearchBarWithStatus onExecute={refreshIfNotUndeclared} />
                        </IfDashboard>
                        <IfSearch>
                          <SearchBarWithStatus onExecute={refreshIfNotUndeclared} />
                        </IfSearch>

                        <QueryBarElements />

                        <IfDashboard>
                          <QueryBar />
                        </IfDashboard>
                      </IfInteractive>
                      <HighlightMessageInQuery query={location.query}>
                        <SearchResult />
                      </HighlightMessageInQuery>
                      <Footer />
                    </SearchArea>
                  </GridContainer>
                </HighlightingRulesProvider>
              </ViewAdditionalContextProvider>
            </DefaultFieldTypesProvider>
          </SearchPageLayoutProvider>
        )}
      </InteractiveContext.Consumer>
    </CurrentViewTypeProvider>
  );
};

ExtendedSearchPage.propTypes = {
  route: PropTypes.object.isRequired,
  location: PropTypes.shape({
    query: PropTypes.object.isRequired,
  }),
  router: PropTypes.object,
  searchRefreshHooks: PropTypes.arrayOf(PropTypes.func).isRequired,
};

ExtendedSearchPage.defaultProps = {
  location: { query: {} },
  router: {
    getCurrentLocation: () => ({ pathname: '', search: '' }),
    push: () => {},
    replace: () => {},
    go: () => {},
    goBack: () => {},
    goForward: () => {},
    setRouteLeaveHook: () => {},
    isActive: () => {},
  },
};

const mapping = {
  searchRefreshHooks: 'views.hooks.searchRefresh',
};

export default withPluginEntities<$Rest<Props, {| router: any |}>, typeof mapping>(withRouter(ExtendedSearchPage), mapping);
