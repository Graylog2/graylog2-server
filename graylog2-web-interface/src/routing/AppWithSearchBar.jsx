import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import {Row, Col} from 'react-bootstrap';

import {Spinner} from 'components/common';
import SearchBar from 'components/search/SearchBar';

import SearchStore from 'stores/search/SearchStore';
import SavedSearchesStore from 'stores/search/SavedSearchesStore';
import CurrentUserStore from 'stores/users/CurrentUserStore';
import StreamsStore from 'stores/streams/StreamsStore';

import SavedSearchesActions from 'actions/search/SavedSearchesActions';

const AppWithSearchBar = React.createClass({
  propTypes: {
    children: PropTypes.element.isRequired,
    params: PropTypes.object,
  },
  mixins: [Reflux.connect(CurrentUserStore), Reflux.connect(SavedSearchesStore)],
  getInitialState() {
    return {
      savedSearches: undefined,
      stream: undefined,
    };
  },
  componentDidMount() {
    SavedSearchesActions.list.triggerPromise();
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
      StreamsStore.get(streamId, (stream) => this.setState({stream: stream}, this._updateSearchParams));
    } else {
      this.setState({stream: undefined}, this._updateSearchParams);
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
        <SearchBar ref="searchBar" userPreferences={this.state.currentUser.preferences} savedSearches={this.state.savedSearches}/>
        <Row id="main-row">
          <Col md={12} id="main-content">
            {this.props.children}
          </Col>
        </Row>
      </div>
    );
  },
});

export default AppWithSearchBar;
