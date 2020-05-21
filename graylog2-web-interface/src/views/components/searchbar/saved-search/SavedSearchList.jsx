// @flow strict
import React from 'react';
import PropTypes from 'prop-types';
import { browserHistory } from 'react-router';
// $FlowFixMe should be there
import styled, { css, type StyledComponent } from 'styled-components';

import Routes from 'routing/Routes';
import { SavedSearchesStore, SavedSearchesActions } from 'views/stores/SavedSearchesStore';
import type { SavedSearchesState } from 'views/stores/SavedSearchesStore';
import connect from 'stores/connect';
import { Alert, Modal, ListGroup, ListGroupItem, Button } from 'components/graylog';
import { Icon, PaginatedList, SearchForm } from 'components/common';
import View from 'views/logic/views/View';
import { type ThemeInterface } from 'theme';

import ViewLoaderContext from 'views/logic/ViewLoaderContext';

type Props = {
  toggleModal: () => void,
  deleteSavedSearch: (View) => Promise<View>,
  views: SavedSearchesState,
};

type State = {
  selectedSavedSearch?: string,
  query: string,
  page: number,
  perPage: number,
};

const AlertIcon: StyledComponent<{}, ThemeInterface, *> = styled(Icon)(({ theme }) => css`
  margin-right: 6px;
  color: ${theme.colors.variant.primary};
`);

const NoSavedSearches: StyledComponent<{}, ThemeInterface, *> = styled(Alert)`
  clear: right;
  display: flex;
  align-items: center;
`;

const DeleteButton: StyledComponent<{}, ThemeInterface, HTMLSpanElement> = styled.span`
  position: absolute;
  top: 10px;
  right: 10px;
`;

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

  onDelete = (e, selectedSavedSearch) => {
    const { views, deleteSavedSearch } = this.props;
    const { list } = views;

    e.stopPropagation();

    if (list) {
      const viewIndex = list.findIndex((v) => v.id === selectedSavedSearch);
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
        <ViewLoaderContext.Consumer key={savedSearch.id}>
          {(loaderFunc) => (
            <ListGroupItem active={selectedSavedSearch === savedSearch.id}
                           onClick={() => this.onLoad(savedSearch.id, loaderFunc)}>
              {savedSearch.title}
              <span>
                <DeleteButton bsSize="xsmall" bsStyle="danger" onClick={(e) => this.onDelete(e, savedSearch.id)}>
                  <Icon name="trash" ttile="Delete" data-testid={`delete-${savedSearch.id}`} />
                </DeleteButton>
              </span>
            </ListGroupItem>
          )}
        </ViewLoaderContext.Consumer>
      );
    });

    const renderResult = ((views && views.list) && views.list.length > 0)
      ? (
        <PaginatedList onChange={this.handlePageChange}
                       activePage={page}
                       totalItems={total}
                       pageSize={perPage}
                       pageSizes={[5, 10, 15]}>
          <ListGroup>{savedSearchList}</ListGroup>
        </PaginatedList>
      )
      : (
        <NoSavedSearches>
          <AlertIcon name="exclamation-triangle" size="lg" />
          <span>No saved searches found.</span>
        </NoSavedSearches>
      );

    return (
      <Modal show>
        <Modal.Body>
          <SearchForm focusAfterMount
                      onSearch={this.handleSearch}
                      onReset={this.handleSearchReset} />
          {renderResult}
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={toggleModal}>Cancel</Button>
        </Modal.Footer>
      </Modal>
    );
  }
}

export default connect(SavedSearchList, { views: SavedSearchesStore });
