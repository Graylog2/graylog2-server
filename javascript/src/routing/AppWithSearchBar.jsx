import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import {Row, Col} from 'react-bootstrap';

import SearchBar from 'components/search/SearchBar';
import SearchStore from 'stores/search/SearchStore';
import CurrentUserStore from 'stores/users/CurrentUserStore';

const AppWithSearchBar = React.createClass({
  propTypes: {
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]).isRequired,
  },
  mixins: [Reflux.connect(CurrentUserStore)],
  render() {
    // TODO: Only show search bar if the user has the right permissions
    // TODO: Check if the search is in a stream
    // TODO: Take care of saved searches
    SearchStore.initializeFieldsFromHash();
    //var searchInStream = searchBarElem.getAttribute('data-search-in-stream');
    //if (searchInStream) {
    //  searchInStream = JSON.parse(searchInStream);
    //  SearchStore.searchInStream = searchInStream;
    //}
    //SavedSearchesStore.updateSavedSearches();

    return (
      <div className="container-fluid">
        <SearchBar userPreferences={this.state.currentUser.permissions}/>
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
