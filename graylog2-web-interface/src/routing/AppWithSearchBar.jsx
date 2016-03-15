import React, { PropTypes } from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import { Spinner } from 'components/common';
import SearchBar from 'components/search/SearchBar';

import SearchStore from 'stores/search/SearchStore';
import SavedSearchesStore from 'stores/search/SavedSearchesStore';
import CurrentUserStore from 'stores/users/CurrentUserStore';
import StreamsStore from 'stores/streams/StreamsStore';
import ConfigurationsStore from 'stores/configurations/ConfigurationsStore';

import SavedSearchesActions from 'actions/search/SavedSearchesActions';
import ConfigurationActions from 'actions/configurations/ConfigurationActions';

const AppWithSearchBar = React.createClass({
  propTypes: {
    children: PropTypes.element.isRequired,
    params: PropTypes.object,
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
  _loadStream(streamId) {
    if (streamId) {
      StreamsStore.get(streamId, (stream) => this.setState({ stream: stream }, this._updateSearchParams));
    } else {
      this.setState({ stream: undefined }, this._updateSearchParams);
    }
  },
  _updateSearchParams() {
    SearchStore.searchInStream = this.state.stream;
    SearchStore.load();
    if (this.refs.searchBar) {
      this.refs.searchBar.reload();
    }
  },
  _isLoading() {
    return !this.state.savedSearches || (this.props.params.streamId && !this.state.stream);
  },
  _decorateChildren(children) {
    return React.Children.map(children, (child) => {
      return React.cloneElement(child, { searchConfig: this.state.searchesClusterConfig });
    });
  },
  render() {
    if (this._isLoading()) {
      return <Spinner/>;
    }

    // TODO: Only show search bar if the user has the right permissions
    // TODO: Check if the search is in a stream
    // TODO: Take care of saved searches
    SearchStore.initializeFieldsFromHash();

    return (
      <div className="container-fluid">
        <SearchBar ref="searchBar" userPreferences={this.state.currentUser.preferences}
                   savedSearches={this.state.savedSearches}
                   config={this.state.searchesClusterConfig} />
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
