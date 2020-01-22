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
  deleteBookmark: (View) => Promise<View>,
  views: SavedSearchesState,
}

type State = {
  selectedBookmark?: string,
  query: string,
  page: number,
  perPage: number,
}

class BookmarkList extends React.Component<Props, State> {
  static propTypes = {
    toggleModal: PropTypes.func.isRequired,
    deleteBookmark: PropTypes.func,
    views: PropTypes.object,
  };

  static defaultProps = {
    views: {},
    deleteBookmark: () => {},
  };

  constructor(props) {
    super(props);

    this.state = {
      selectedBookmark: undefined,
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

  onLoad = (selectedBookmark, loadFunc) => {
    const { toggleModal } = this.props;
    if (!selectedBookmark || !loadFunc) {
      return;
    }
    loadFunc(selectedBookmark).then(() => {
      browserHistory.push(Routes.pluginRoute('SEARCH_VIEWID')(selectedBookmark));
    });
    toggleModal();
  };

  onDelete = (selectedBookmark) => {
    const { views, deleteBookmark } = this.props;
    const { list } = views;
    if (list) {
      const viewIndex = list.findIndex(v => v.id === selectedBookmark);
      if (viewIndex < 0) {
        return;
      }

      // eslint-disable-next-line no-alert
      if (window.confirm(`You are about to delete saved search: "${list[viewIndex].title}". Are you sure?`)) {
        deleteBookmark(list[viewIndex]).then(() => {
          this.execSearch();
        });
      }
    }
  };

  render() {
    const { views, toggleModal } = this.props;
    const { total, page, perPage = 5 } = views.pagination;
    const { selectedBookmark } = this.state;
    const bookmarkList = (views.list || []).map((bookmark) => {
      return (
        <ListGroupItem active={selectedBookmark === bookmark.id}
                       onClick={() => this.setState({ selectedBookmark: bookmark.id })}
                       key={bookmark.id}>
          {bookmark.title}
        </ListGroupItem>
      );
    });

    const renderResult = (views && views.list) && views.list.length > 0
      ? (<ListGroup>{bookmarkList}</ListGroup>)
      : (<span>No bookmarks found</span>);

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
              <Button disabled={!selectedBookmark}
                      bsStyle="primary"
                      onClick={() => { this.onLoad(selectedBookmark, loaderFunc); }}>
                Load
              </Button>
            )}
          </ViewLoaderContext.Consumer>
          <Button disabled={!selectedBookmark}
                  onClick={() => { this.onDelete(selectedBookmark); }}>
            Delete
          </Button>
          <Button onClick={toggleModal}>Cancel</Button>
        </Modal.Footer>
      </Modal>
    );
  }
}

export default connect(BookmarkList, { views: SavedSearchesStore });
