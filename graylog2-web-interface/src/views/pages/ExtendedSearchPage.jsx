// @flow strict
import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import { Row } from 'components/graylog';
import * as Immutable from 'immutable';

import connect from 'stores/connect';
import WithSearchStatus from 'views/components/WithSearchStatus';
import SearchResult from 'views/components/SearchResult';
import type {
  SearchRefreshCondition,
  SearchRefreshConditionArguments,
} from 'views/logic/hooks/SearchRefreshCondition';
import { FieldTypesActions } from 'views/stores/FieldTypesStore';
import { SearchConfigActions } from 'views/stores/SearchConfigStore';

import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';
import { SearchMetadataActions } from 'views/stores/SearchMetadataStore';
import { SearchActions } from 'views/stores/SearchStore';
import { StreamsActions } from 'views/stores/StreamsStore';
import { ViewActions, ViewStore } from 'views/stores/ViewStore';
import CustomPropTypes from 'views/components/CustomPropTypes';
import HeaderElements from 'views/components/HeaderElements';
import QueryBarElements from 'views/components/QueryBarElements';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import WindowLeaveMessage from 'views/components/common/WindowLeaveMessage';
import withPluginEntities from 'views/logic/withPluginEntities';
import IfDashboard from 'views/components/dashboard/IfDashboard';
import QueryBar from 'views/components/QueryBar';
import DashboardSearchBar from 'views/components/DashboardSearchBar';
import SearchBar from 'views/components/SearchBar';
import CurrentViewTypeProvider from 'views/components/views/CurrentViewTypeProvider';

// eslint-disable-next-line import/no-webpack-loader-syntax
import style from '!style/useable!css!./ExtendedSearchPage.css';
import IfSearch from '../components/search/IfSearch';
import { AdditionalContext } from '../logic/ActionContext';

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

const ExtendedSearchPage = ({ executionState, route, searchRefreshHooks }) => {
  const refreshIfNotUndeclared = view => _refreshIfNotUndeclared(searchRefreshHooks, executionState, view);
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
    }, () => {
    });

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
      <HeaderElements />
      <Row id="main-row">
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
      </Row>
    </CurrentViewTypeProvider>
  );
};

ExtendedSearchPage.propTypes = {
  executionState: CustomPropTypes.instanceOf(SearchExecutionState).isRequired,
  route: PropTypes.object.isRequired,
  searchRefreshHooks: PropTypes.arrayOf(PropTypes.func).isRequired,
};

const ExtendedSearchPageWithExecutionState = connect(ExtendedSearchPage, { executionState: SearchExecutionStateStore });

const mapping = {
  searchRefreshHooks: 'views.hooks.searchRefresh',
};

export default withPluginEntities<Props, typeof mapping>(ExtendedSearchPageWithExecutionState, mapping);
