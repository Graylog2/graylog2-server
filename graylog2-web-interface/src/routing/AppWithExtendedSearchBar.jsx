import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Spinner } from 'components/common';

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

const AppWithExtendedSearchBar = createReactClass({
  displayName: 'AppWithExtendedSearchBar',

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
      searchesClusterConfig: undefined,
    };
  },

  componentDidMount() {
    const { params } = this.props;
    SavedSearchesActions.list.triggerPromise();
    ConfigurationActions.listSearchesClusterConfig();
    this._loadStream(params.streamId);
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
    const { stream } = this.state;
    SearchStore.searchInStream = stream;
    SearchStore.load();
    if (this.searchBar) {
      this.searchBar.reload();
    }
  },

  _isLoading() {
    const { stream, savedSearches, searchesClusterConfig } = this.state;
    const { params } = this.props;
    return !savedSearches || !searchesClusterConfig || (params.streamId && !stream);
  },

  _decorateChildren(children) {
    return React.Children.map(children, (child) => {
      const { searchesClusterConfig, forceFetch } = this.state;
      return React.cloneElement(child, { searchConfig: searchesClusterConfig, forceFetch: forceFetch });
    });
  },

  _searchBarShouldDisplayRefreshControls() {
    const { location } = this.props;
    // Hide refresh controls on sources page
    return location.pathname !== Routes.SOURCES;
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

    const { children } = this.props;
    return (
      <div>
        {children}
      </div>
    );
  },
});

export default AppWithExtendedSearchBar;
