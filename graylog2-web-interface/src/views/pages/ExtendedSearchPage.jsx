// @flow strict
import React, { useEffect, useState } from 'react';
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
import { SelectedFieldsStore } from 'views/stores/SelectedFieldsStore';

import type { QueryId } from 'views/logic/queries/Query';
import TSearchResult from 'views/logic/SearchResult';
import DashboardSearchBar from 'views/components/DashboardSearchBar';
import SearchBar from 'views/components/SearchBar';
import CurrentViewTypeProvider from 'views/components/views/CurrentViewTypeProvider';
import IfSearch from 'views/components/search/IfSearch';
import { AdditionalContext } from 'views/logic/ActionContext';

// eslint-disable-next-line import/no-webpack-loader-syntax
import style from '!style/useable!css!./ExtendedSearchPage.css';
import Spinner from '../../components/common/Spinner';

const ConnectedSideBar = connect(SideBar, { viewMetadata: ViewMetadataStore });
const ConnectedFieldList = connect(FieldList, { selectedFields: SelectedFieldsStore });

type Props = {
  route: any,
  queryId: QueryId,
  searches: TSearchResult,
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

const ExtendedSearchPage = ({ fieldTypes, queryId, searches, route, searchRefreshHooks }) => {
  const refreshIfNotUndeclared = view => _refreshIfNotUndeclared(searchRefreshHooks, SearchExecutionStateStore.getInitialState(), view);
  const results = searches && searches.result;
  const currentResults = results ? results.forId(queryId) : undefined;
  const queryFields = fieldTypes.queryFields.get(queryId, fieldTypes.all);

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

  const sidebar = currentResults
    ? (
      <ConnectedSideBar queryId={queryId}
                        results={currentResults}>
        <ConnectedFieldList allFields={fieldTypes.all}
                            fields={queryFields} />
      </ConnectedSideBar>
    )
    : <Spinner />;


  return (
    <CurrentViewTypeProvider>
      <IfDashboard>
        <WindowLeaveMessage route={route} />
      </IfDashboard>
      <div id="main-row" className="grid-container">
        {sidebar}
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
        </div>
      </div>
    </CurrentViewTypeProvider>
  );
};

ExtendedSearchPage.propTypes = {
  route: PropTypes.object.isRequired,
  searchRefreshHooks: PropTypes.arrayOf(PropTypes.func).isRequired,
  fieldTypes: PropTypes.object.isRequired,
  searches: PropTypes.object.isRequired,
  queryId: PropTypes.string.isRequired,
};

const ConnectedExtendedSearchPage = connect(ExtendedSearchPage, {
  fieldTypes: FieldTypesStore,
  searches: SearchStore,
  viewMetadata: ViewMetadataStore,
}, props => Object.assign(
  {},
  props,
  {
    searches: { result: props.searches.result, widgetMapping: props.searches.widgetMapping },
    queryId: props.viewMetadata.activeQuery,
  },
));

const mapping = {
  searchRefreshHooks: 'views.hooks.searchRefresh',
};

export default withPluginEntities<Props, typeof mapping>(ConnectedExtendedSearchPage, mapping);
