import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import { Spinner } from 'components/common';
import SearchBar from 'components/search/SearchBar';

import Routes from 'routing/Routes';

import StoreProvider from 'injection/StoreProvider';

import ActionsProvider from 'injection/ActionsProvider';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const SearchStore = StoreProvider.getStore('Search');
const SavedSearchesStore = StoreProvider.getStore('SavedSearches');
const StreamsStore = StoreProvider.getStore('Streams');
const ConfigurationsStore = StoreProvider.getStore('Configurations');
const SavedSearchesActions = ActionsProvider.getActions('SavedSearches');
const ConfigurationActions = ActionsProvider.getActions('Configuration');

const AppWithSearchBar = createReactClass({
  displayName: 'AppWithSearchBar',

  propTypes: {
    children: PropTypes.element.isRequired,
    location: PropTypes.object,
    params: PropTypes.object,
  },

  mixins: [
    Reflux.connect(CurrentUserStore),
    Reflux.connect(SavedSearchesStore),
    Reflux.connect(ConfigurationsStore),
  ],

  getInitialState() {
    return {
      forceFetch: false,
      savedSearches: undefined,
      stream: undefined,
    };
  },

  componentDidMount() {
    SavedSearchesActions.list.triggerPromise();
    ConfigurationActions.listSearchesClusterConfig();
    this._loadStream(this.props.params.streamId);
  },

  componentWillReceiveProps(nextProps) {
    this._loadStream(nextProps.params.streamId);
  },

  componentWillUnmount() {
    SearchStore.unload();
  },

  _resetForceFetch() {
    this.setState({ forceFetch: false });
  },

  _loadStream(streamId) {
    if (streamId) {
      StreamsStore.get(streamId, stream => this.setState({ stream: stream }, this._updateSearchParams));
    } else {
      this.setState({ stream: undefined }, this._updateSearchParams);
    }
  },

  _updateSearchParams() {
    SearchStore.searchInStream = this.state.stream;
    SearchStore.load();
    if (this.searchBar) {
      this.searchBar.reload();
    }
  },

  _isLoading() {
    return !this.state.savedSearches || !this.state.searchesClusterConfig || (this.props.params.streamId && !this.state.stream);
  },

  _decorateChildren(children) {
    return React.Children.map(children, (child) => {
      return React.cloneElement(child, { searchConfig: this.state.searchesClusterConfig, forceFetch: this.state.forceFetch });
    });
  },

  _searchBarShouldDisplayRefreshControls() {
    // Hide refresh controls on sources page
    return this.props.location.pathname !== Routes.SOURCES;
  },

  _onExecuteSearch() {
    this.setState({ forceFetch: true }, this._resetForceFetch);
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    // TODO: Only show search bar if the user has the right permissions
    SearchStore.load();
    SearchStore.initializeFieldsFromHash();

    return (
      <div className="container-fluid">
        <SearchBar ref={(searchBar) => { this.searchBar = searchBar; }}
                   userPreferences={this.state.currentUser.preferences}
                   savedSearches={this.state.savedSearches}
                   config={this.state.searchesClusterConfig}
                   displayRefreshControls={this._searchBarShouldDisplayRefreshControls()}
                   onExecuteSearch={this._onExecuteSearch} />
        <Row id="main-row">
          <Col md={12} id="main-content">
            {this._decorateChildren(this.props.children)}
          </Col>
        </Row>
      </div>
    );
  },
});

export default AppWithSearchBar;
