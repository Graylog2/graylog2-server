// @flow strict
import React from 'react';
import { SavedSearchesStore, SavedSearchesActions } from 'views/stores/SavedSearchesStore';
import type { SavedSearchesState } from 'views/stores/SavedSearchesStore';
import connect from 'stores/connect';
import { Popover, ListGroup, ListGroupItem, Button } from 'react-bootstrap';
import { PaginatedList, SearchForm } from 'components/common';
import { Portal } from 'react-portal';
import { Position } from 'react-overlays';

import ViewLoaderContext from 'views/logic/ViewLoaderContext';

type Props = {
  toggleModal: () => void,
  views: SavedSearchesState,
  target: any,
}

type State = {
  selectedBookmark?: string,
  query: string,
  page: number,
  perPage: number,
}

class BookmarkList extends React.Component<Props, State> {
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
    if (!selectedBookmark) {
      return;
    }
    loadFunc(selectedBookmark);
  };

  render() {
    const { views, target, toggleModal } = this.props;
    const { total, page, perPage = 5 } = views.pagination;
    const { selectedBookmark } = this.state;
    const bookmarkList = (views.list || []).map((bookmark) => {
      return (
        <ListGroupItem active={selectedBookmark === bookmark.id}
                       onClick={() => this.setState({ selectedBookmark: bookmark.id })}
                       header={bookmark.title}
                       key={bookmark.id}>
          {bookmark.summary}
        </ListGroupItem>
      );
    });

    const renderResult = (views && views.list) && views.list.length > 0
      ? (<ListGroup>{bookmarkList}</ListGroup>)
      : (<span>No bookmarks found</span>);

    return (
      <Portal>
        <Position container={document.body}
                  placement="left"
                  target={target}>
          <Popover title="Name of search" id="bookmark-popover">
            <SearchForm onSearch={this.handleSearch}
                        onReset={this.handleSearchReset} />
            <PaginatedList onChange={this.handlePageChange}
                           activePage={page}
                           totalItems={total}
                           pageSize={perPage}
                           pageSizes={[5, 10, 15]}>
              {renderResult}

              <ViewLoaderContext.Consumer>
                {({ loaderFunc }) => <Button disabled={!selectedBookmark} onClick={() => { this.onLoad(selectedBookmark, loaderFunc); }}>Load</Button> }
              </ViewLoaderContext.Consumer>
              <Button onClick={toggleModal}>Cancel</Button>
            </PaginatedList>
          </Popover>
        </Position>
      </Portal>
    );
  }
}

export default connect(BookmarkList, { views: SavedSearchesStore });
