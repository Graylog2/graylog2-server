// @flow strict
import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';

import connect from 'stores/connect';
import SideBar from 'views/components/sidebar/SideBar';
import WithSearchStatus from 'views/components/WithSearchStatus';
import SearchResult from 'views/components/SearchResult';
import type {
  SearchRefreshCondition,
  SearchRefreshConditionArguments,
} from 'views/logic/hooks/SearchRefreshCondition';
import Footer from 'components/layout/Footer';

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

// eslint-disable-next-line import/no-webpack-loader-syntax
import style from '!style/useable!css!./ExtendedSearchPage.css';

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
};

const _searchRefreshConditionChain = (searchRefreshHooks, state: SearchRefreshConditionArguments) => {
  if (!searchRefreshHooks || searchRefreshHooks.length === 0) {
    return true;
  }
  return searchRefreshHooks.every((condition: SearchRefreshCondition) => condition(state));
};

const _refreshIfNotUndeclared = (searchRefreshHooks, executionState, view) => {
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

const ExtendedSearchPage = ({ route, searchRefreshHooks }: Props) => {
  const refreshIfNotUndeclared = view => _refreshIfNotUndeclared(searchRefreshHooks, SearchExecutionStateStore.getInitialState(), view);

  useEffect(() => {
    style.use();

    SearchConfigActions.refresh();
    FieldTypesActions.all();
    const { view } = ViewStore.getInitialState();
    let storeListenersUnsubscribes = Immutable.List();
    refreshIfNotUndeclared(view).then(() => {
      storeListenersUnsubscribes = storeListenersUnsubscribes
        .push(SearchActions.refresh.listen(() => {
          const { view: currentView } = ViewStore.getInitialState();
          refreshIfNotUndeclared(currentView);
        }))
        .push(ViewActions.search.completed.listen(refreshIfNotUndeclared));
      return null;
    }, () => { });

    StreamsActions.refresh();

    // Returning cleanup function used when unmounting
    return () => {
      style.unuse();
      storeListenersUnsubscribes.forEach(unsubscribeFunc => unsubscribeFunc());
    };
  }, []);

  return (
    <CurrentViewTypeProvider>
      <IfDashboard>
        <WindowLeaveMessage route={route} />
      </IfDashboard>
      <div id="main-row" className="grid-container">
        <ConnectedSideBar>
          <ConnectedFieldList />
        </ConnectedSideBar>
        <div className="search-grid">
          <HeaderElements />
          <IfDashboard>
            <DashboardSearchBarWithStatus onExecute={refreshIfNotUndeclared} />
            <QueryBar />
          </IfDashboard>
          <IfSearch>
            <SearchBarWithStatus onExecute={refreshIfNotUndeclared} />
          </IfSearch>

          <QueryBarElements />

          <ViewAdditionalContextProvider>
            <SearchResult />
          </ViewAdditionalContextProvider>
          <Footer />
        </div>
      </div>
    </CurrentViewTypeProvider>
  );
};

ExtendedSearchPage.propTypes = {
  route: PropTypes.object.isRequired,
  searchRefreshHooks: PropTypes.arrayOf(PropTypes.func).isRequired,
};

const mapping = {
  searchRefreshHooks: 'views.hooks.searchRefresh',
};

export default withPluginEntities<Props, typeof mapping>(ExtendedSearchPage, mapping);
