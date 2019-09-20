import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Spinner } from 'components/common';

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
    params: PropTypes.shape({
      streamId: PropTypes.string,
    }).isRequired,
  },

  mixins: [
    Reflux.connect(CurrentUserStore),
    Reflux.connect(SavedSearchesStore),
    Reflux.connect(ConfigurationsStore),
  ],

  getInitialState() {
    return {
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
    const { streamId } = params;
    return !savedSearches || !searchesClusterConfig || (streamId && !stream);
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
