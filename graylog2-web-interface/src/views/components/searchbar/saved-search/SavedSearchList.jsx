// @flow strict
import React from 'react';
import PropTypes from 'prop-types';
import { browserHistory } from 'react-router';

import Routes from 'routing/Routes';
import { SavedSearchesStore, SavedSearchesActions } from 'views/stores/SavedSearchesStore';
import type { SavedSearchesState } from 'views/stores/SavedSearchesStore';
import connect from 'stores/connect';
import { Modal, ListGroup, ListGroupItem, Button } from 'components/graylog';
import { PaginatedList, SearchForm } from 'components/common';
import View from 'views/logic/views/View';

import ViewLoaderContext from 'views/logic/ViewLoaderContext';

type Props = {
  toggleModal: () => void,
  deleteSavedSearch: (View) => Promise<View>,
  views: SavedSearchesState,
}

type State = {
  selectedSavedSearch?: string,
  query: string,
  page: number,
  perPage: number,
}

class SavedSearchList extends React.Component<Props, State> {
  static propTypes = {
    toggleModal: PropTypes.func.isRequired,
    deleteSavedSearch: PropTypes.func.isRequired,
    views: PropTypes.object,
  };

  static defaultProps = {
    views: {},
  };

  constructor(props) {
    super(props);

    this.state = {
      selectedSavedSearch: undefined,
      query: '',
      page: 1,
      perPage: 5,
    };
  }

  componentDidMount() {
    this.execSearch();
  }

  execSearch = () => {
    const { query, page, perPage } = this.state;
    SavedSearchesActions.search(query, page, perPage);
  };

  handlePageChange = (page, perPage) => {
    this.setState({ page: page, perPage: perPage }, this.execSearch);
  };

  handleSearch = (query) => {
    this.setState({ query: query, page: 1 }, this.execSearch);
  };

  handleSearchReset = () => {
    this.setState({ query: '', page: 1 }, this.execSearch);
  };

  onLoad = (selectedSavedSearch, loadFunc) => {
    const { toggleModal } = this.props;
    if (!selectedSavedSearch || !loadFunc) {
      return;
    }
    loadFunc(selectedSavedSearch).then(() => {
      browserHistory.push(Routes.pluginRoute('SEARCH_VIEWID')(selectedSavedSearch));
    });
    toggleModal();
  };

  onDelete = (selectedSavedSearch) => {
    const { views, deleteSavedSearch } = this.props;
    const { list } = views;
    if (list) {
      const viewIndex = list.findIndex(v => v.id === selectedSavedSearch);
      if (viewIndex < 0) {
        return;
      }

      // eslint-disable-next-line no-alert
      if (window.confirm(`You are about to delete saved search: "${list[viewIndex].title}". Are you sure?`)) {
        deleteSavedSearch(list[viewIndex]).then(() => {
          this.execSearch();
        });
      }
    }
  };

  render() {
    const { views, toggleModal } = this.props;
    const { total, page, perPage = 5 } = views.pagination;
    const { selectedSavedSearch } = this.state;
    const savedSearchList = (views.list || []).map((savedSearch) => {
      return (
        <ListGroupItem active={selectedSavedSearch === savedSearch.id}
                       onClick={() => this.setState({ selectedSavedSearch: savedSearch.id })}
                       key={savedSearch.id}>
          {savedSearch.title}
        </ListGroupItem>
      );
    });

    const renderResult = (views && views.list) && views.list.length > 0
      ? (<ListGroup>{savedSearchList}</ListGroup>)
      : (<span>No saved searches found</span>);

    return (
      <Modal show>
        <Modal.Body>
          <SearchForm onSearch={this.handleSearch}
                      onReset={this.handleSearchReset} />
          <PaginatedList onChange={this.handlePageChange}
                         activePage={page}
                         totalItems={total}
                         pageSize={perPage}
                         pageSizes={[5, 10, 15]}>
            {renderResult}
          </PaginatedList>
        </Modal.Body>
        <Modal.Footer>
          <ViewLoaderContext.Consumer>
            {loaderFunc => (
              <Button disabled={!selectedSavedSearch}
                      bsStyle="primary"
                      onClick={() => { this.onLoad(selectedSavedSearch, loaderFunc); }}>
                Load
              </Button>
            )}
          </ViewLoaderContext.Consumer>
          <Button disabled={!selectedSavedSearch}
                  onClick={() => { this.onDelete(selectedSavedSearch); }}>
            Delete
          </Button>
          <Button onClick={toggleModal}>Cancel</Button>
        </Modal.Footer>
      </Modal>
    );
  }
}

export default connect(SavedSearchList, { views: SavedSearchesStore });
