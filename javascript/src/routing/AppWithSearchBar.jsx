import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import {Row, Col} from 'react-bootstrap';

import {Spinner} from 'components/common';
import SearchBar from 'components/search/SearchBar';

import SearchStore from 'stores/search/SearchStore';
import SavedSearchesStore from 'stores/search/SavedSearchesStore';
import CurrentUserStore from 'stores/users/CurrentUserStore';

import SavedSearchesActions from 'actions/search/SavedSearchesActions';

const AppWithSearchBar = React.createClass({
  propTypes: {
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]).isRequired,
  },
  mixins: [Reflux.connect(CurrentUserStore), Reflux.connect(SavedSearchesStore)],
  getInitialState() {
    return {
      savedSearches: undefined,
    };
  },
  componentDidMount() {
    SavedSearchesActions.list.triggerPromise();
  },
  _isLoading() {
    return !this.state.savedSearches;
  },
  render() {
    if (this._isLoading()) {
      return <Spinner/>;
    }

    // TODO: Only show search bar if the user has the right permissions
    // TODO: Check if the search is in a stream
    // TODO: Take care of saved searches
    SearchStore.initializeFieldsFromHash();
    //var searchInStream = searchBarElem.getAttribute('data-search-in-stream');
    //if (searchInStream) {
    //  searchInStream = JSON.parse(searchInStream);
    //  SearchStore.searchInStream = searchInStream;
    //}

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
