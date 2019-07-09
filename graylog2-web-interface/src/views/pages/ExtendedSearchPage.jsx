// @flow strict
import * as React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import { Row } from 'react-bootstrap';
import * as Immutable from 'immutable';

import connect from 'stores/connect';
import QueryBar from 'views/components/QueryBar';
import SearchBarWithStatus from 'views/components/SearchBarWithStatus';
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
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import WindowLeaveMessage from 'views/components/common/WindowLeaveMessage';
import withPluginEntities from 'views/logic/withPluginEntities';

// eslint-disable-next-line import/no-webpack-loader-syntax
import style from '!style/useable!css!./ExtendedSearchPage.css';

type Props = {
  headerElements: Array<React.ComponentType<{}>>,
  queryBarElements: Array<React.ComponentType<{}>>,
  route: any,
  searchRefreshHooks: Array<SearchRefreshCondition>,
};

const ExtendedSearchPage = createReactClass({
  displayName: 'ExtendedSearchPage',

  propTypes: {
    executionState: PropTypes.instanceOf(SearchExecutionState).isRequired,
    headerElements: PropTypes.arrayOf(PropTypes.func).isRequired,
    queryBarElements: PropTypes.arrayOf(PropTypes.func).isRequired,
    route: PropTypes.object.isRequired,
    searchRefreshHooks: PropTypes.arrayOf(PropTypes.func).isRequired,
  },

  componentDidMount() {
    style.use();

    SearchConfigActions.refresh();
    FieldTypesActions.all();
    const { view } = ViewStore.getInitialState();
    this._refreshIfNotUndeclared(view).then(() => {
      this.storeListenersUnsubscribes = this.storeListenersUnsubscribes
        .push(SearchActions.refresh.listen(() => {
          const { view: currentView } = ViewStore.getInitialState();
          this._refreshIfNotUndeclared(currentView);
        }))
        .push(ViewActions.search.completed.listen(this._refreshIfNotUndeclared));
      return null;
    }, () => {});

    StreamsActions.refresh();
  },

  componentWillUnmount() {
    style.unuse();
    this.storeListenersUnsubscribes.forEach(unsubscribeFunc => unsubscribeFunc());
  },

  storeListenersUnsubscribes: Immutable.List(),

  _searchRefreshConditionChain(state: SearchRefreshConditionArguments) {
    const { searchRefreshHooks } = this.props;
    if (!searchRefreshHooks || searchRefreshHooks.length === 0) {
      return true;
    }
    return searchRefreshHooks.every((condition: SearchRefreshCondition) => condition(state));
  },

  _refreshIfNotUndeclared(view) {
    const { executionState } = this.props;
    return SearchMetadataActions.parseSearch(view.search).then((searchMetadata) => {
      if (this._searchRefreshConditionChain({ view, searchMetadata, executionState })) {
        FieldTypesActions.all();
        return SearchActions.execute(executionState);
      }
      return Promise.reject(searchMetadata);
    });
  },

  render() {
    const { headerElements = [], queryBarElements = [], route } = this.props;
    return (
      <React.Fragment>
        <WindowLeaveMessage route={route} />
        {/* eslint-disable-next-line react/no-array-index-key */}
        {headerElements.map((Component, idx) => <Component key={idx} />)}
        <Row id="main-row">
          <QueryBar>
            <SearchBarWithStatus onExecute={this._refreshIfNotUndeclared} />

            {/* eslint-disable-next-line react/no-array-index-key */}
            {queryBarElements.map((Component, idx) => <Component key={idx} />)}

            <SearchResult />
          </QueryBar>
        </Row>
      </React.Fragment>
    );
  },
});

const ExtendedSearchPageWithExecutionState = connect(ExtendedSearchPage, { executionState: SearchExecutionStateStore });

const mapping = {
  queryBarElements: 'views.elements.queryBar',
  headerElements: 'views.elements.header',
  searchRefreshHooks: 'views.hooks.searchRefresh',
};

export default withPluginEntities<Props, typeof mapping>(ExtendedSearchPageWithExecutionState, mapping);
