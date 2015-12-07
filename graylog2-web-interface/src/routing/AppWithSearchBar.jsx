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
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]).isRequired,
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
    if (this.props.params.streamId) {
      StreamsStore.get(this.props.params.streamId, (stream) => this.setState({stream: stream}));
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
    if (this.state.stream) {
      SearchStore.searchInStream = this.state.stream;
    }

    return (
      <div className="container-fluid">
        <SearchBar userPreferences={this.state.currentUser.preferences} savedSearches={this.state.savedSearches}/>
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
