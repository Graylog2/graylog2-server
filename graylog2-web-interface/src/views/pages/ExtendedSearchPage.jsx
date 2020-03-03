// @flow strict
import React, { useEffect } from 'react';
import type { ComponentType } from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import styled, { css } from 'styled-components';
import { withRouter } from 'react-router';

import connect from 'stores/connect';
import Footer from 'components/layout/Footer';

import SideBar from 'views/components/sidebar/SideBar';
import WithSearchStatus from 'views/components/WithSearchStatus';
import SearchResult from 'views/components/SearchResult';
import type {
  SearchRefreshCondition,
  SearchRefreshConditionArguments,
} from 'views/logic/hooks/SearchRefreshCondition';

import { Grid } from 'components/graylog';
import { FieldTypesStore, FieldTypesActions } from 'views/stores/FieldTypesStore';
import { SearchStore, SearchActions } from 'views/stores/SearchStore';
import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';
import { SearchConfigActions } from 'views/stores/SearchConfigStore';
import { SearchMetadataActions } from 'views/stores/SearchMetadataStore';
import { StreamsActions } from 'views/stores/StreamsStore';
import { ViewActions, ViewStore } from 'views/stores/ViewStore';
import HeaderElements from 'views/components/HeaderElements';
import QueryBarElements from 'views/components/QueryBarElements';
import WindowLeaveMessage from 'views/components/common/WindowLeaveMessage';
import withPluginEntities from 'views/logic/withPluginEntities';
import IfDashboard from 'views/components/dashboard/IfDashboard';
import QueryBar from 'views/components/QueryBar';
import { ViewMetadataStore } from 'views/stores/ViewMetadataStore';
import { FieldList } from 'views/components/sidebar';

import DashboardSearchBar from 'views/components/DashboardSearchBar';
import SearchBar from 'views/components/SearchBar';
import CurrentViewTypeProvider from 'views/components/views/CurrentViewTypeProvider';
import IfSearch from 'views/components/search/IfSearch';
import { AdditionalContext } from 'views/logic/ActionContext';
import IfInteractive from 'views/components/dashboard/IfInteractive';
import InteractiveContext from 'views/components/contexts/InteractiveContext';
import bindSearchParamsFromQuery from 'views/hooks/BindSearchParamsFromQuery';
import { useSyncWithQueryParameters } from 'views/hooks/SyncWithQueryParameters';

// eslint-disable-next-line import/no-webpack-loader-syntax
import style from '!style/useable!css!./ExtendedSearchPage.css';
import HighlightMessageInQuery from '../components/messagelist/HighlightMessageInQuery';

const GridContainer: ComponentType<{ interactive: boolean }> = styled.div`
  ${({ interactive }) => (interactive ? css`
    display: grid;
    grid-template-rows: 1fr;
    grid-template-columns: 50px 250px 1fr;
    grid-template-areas: "sidebar search";
  ` : '')}
`;

const SearchArea = styled.div`
  grid-area: search;
  grid-column-start: 2;
  grid-column-end: 4;
  padding: 15px;

  z-index: 1;
`;

const SearchGrid = styled(Grid)`
  width: 100%;
`;

const ConnectedSideBar = connect(SideBar, { viewMetadata: ViewMetadataStore, searches: SearchStore },
  props => Object.assign(
    {},
    props,
    {
      queryId: props.viewMetadata.activeQuery,
      results: props.searches && props.searches.result ? props.searches.result.forId(props.viewMetadata.activeQuery) : undefined,
    },
  ));
const ConnectedFieldList = connect(FieldList, { fieldTypes: FieldTypesStore, viewMetadata: ViewMetadataStore },
  props => Object.assign(
    {},
    props,
    {
      allFields: props.fieldTypes.all,
      fields: props.fieldTypes.queryFields.get(props.viewMetadata.activeQuery, props.fieldTypes.all),
    },
  ));

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

const ViewAdditionalContextProvider = connect(AdditionalContext.Provider, { view: ViewStore }, ({ view }) => ({ value: { view: view.view } }));

const useStyle = () => {
  useEffect(() => {
    style.use();
    return () => style.unuse();
  }, []);
};

const ExtendedSearchPage = ({ route, location = { query: {} }, router, searchRefreshHooks }: Props) => {
  const { pathname, search } = router.getCurrentLocation();
  const query = `${pathname}${search}`;
  const refreshIfNotUndeclared = () => _refreshIfNotUndeclared(searchRefreshHooks, SearchExecutionStateStore.getInitialState());

  useEffect(() => {
    const { view } = ViewStore.getInitialState();

    bindSearchParamsFromQuery({ view, query: location.query, retry: () => Promise.resolve() });
  }, [query]);

  useStyle();

  useEffect(() => {
    SearchConfigActions.refresh();
    FieldTypesActions.all();
    StreamsActions.refresh();

    let storeListenersUnsubscribes = Immutable.List();
    refreshIfNotUndeclared().then(() => {
      storeListenersUnsubscribes = storeListenersUnsubscribes
        .push(SearchActions.refresh.listen(refreshIfNotUndeclared))
        .push(ViewActions.search.completed.listen(refreshIfNotUndeclared));
      return null;
    }, () => { });

    // Returning cleanup function used when unmounting
    return () => storeListenersUnsubscribes.forEach(unsubscribeFunc => unsubscribeFunc());
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
        {interactive => (
          <GridContainer id="main-row" interactive={interactive}>
            <IfInteractive>
              <ConnectedSideBar>
                <ConnectedFieldList />
              </ConnectedSideBar>
            </IfInteractive>
            <SearchArea>
              <SearchGrid>
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

                <ViewAdditionalContextProvider>
                  <HighlightMessageInQuery query={location.query}>
                    <SearchResult />
                  </HighlightMessageInQuery>
                </ViewAdditionalContextProvider>
                <Footer />
              </SearchGrid>
            </SearchArea>
          </GridContainer>
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
