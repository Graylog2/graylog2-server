/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import type { PaginatedViews } from 'views/stores/ViewManagementStore';
import { SavedSearchesActions } from 'views/stores/SavedSearchesStore';
import { Alert, Modal, ListGroup, ListGroupItem, Button } from 'components/graylog';
import { Icon, PaginatedList, SearchForm, Spinner } from 'components/common';
import View from 'views/logic/views/View';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import UserNotification from 'util/UserNotification';

type Props = {
  toggleModal: () => void,
  deleteSavedSearch: (view: View) => Promise<View>,
  activeSavedSearchId: string,
};

const StyledListGroup = styled(ListGroup)`
  clear: both;
`;

const NoSavedSearches = styled(Alert)`
  clear: right;
  display: flex;
  align-items: center;
  margin-top: 15px;
`;

const ListContainer = styled.div`
  margin-top: 15px;
`;

const LoadingSpinner = styled(Spinner)`
  margin-top: 15px;
`;

const DeleteButton = styled.span(({ theme }) => `
  position: absolute;
  margin-right: 10px;
  width: 25px;
  height: 25px;
  right: 0;
  top: 5px;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  color: ${theme.colors.gray[60]};
`);

const DEFAULT_PAGINATION = {
  query: '',
  page: 1,
  perPage: 10,
  count: 0,
};

const onLoad = (toggleModal, selectedSavedSearchId, loadFunc) => {
  if (!selectedSavedSearchId || !loadFunc) {
    return;
  }

  loadFunc(selectedSavedSearchId);

  toggleModal();
};

const onDelete = (e, savedSearches, deleteSavedSearch, selectedSavedSearchId) => {
  e.stopPropagation();

  const selectedSavedSearch = savedSearches?.find((savedSearch) => savedSearch.id === selectedSavedSearchId);

  if (savedSearches) {
    // eslint-disable-next-line no-alert
    if (window.confirm(`You are about to delete saved search: "${selectedSavedSearch.title}". Are you sure?`)) {
      deleteSavedSearch(selectedSavedSearch);
    }
  }
};

const _loadSavesSearches = (pagination, setLoading, setPaginatedSavedSearches) => {
  setLoading(true);

  SavedSearchesActions.search(pagination).then((paginatedSavedSearches) => {
    setPaginatedSavedSearches(paginatedSavedSearches);
    setLoading(false);
  }).catch((error) => {
    UserNotification.error(`Fetching saved searches failed with status: ${error}`,
      'Could not retrieve saved searches');
  });
};

const _updateListOnSearchDelete = (perPage, query, setPagination) => ViewManagementActions.delete.completed.listen(() => setPagination({ page: DEFAULT_PAGINATION.page, perPage, query }));

const SavedSearchList = ({ toggleModal, deleteSavedSearch, activeSavedSearchId }: Props) => {
  const [paginatedSavedSearches, setPaginatedSavedSearches] = useState<PaginatedViews | undefined>();
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  const [loading, setLoading] = useState(false);
  const { page, query, perPage } = pagination;
  const { list: savedSearches, pagination: { total = 0 } = {} } = paginatedSavedSearches || {};

  useEffect(() => _loadSavesSearches(pagination, setLoading, setPaginatedSavedSearches), [pagination]);
  useEffect(() => _updateListOnSearchDelete(perPage, query, setPagination), [perPage, query]);

  const handleSearch = (newQuery: string) => setPagination({ ...pagination, query: newQuery, page: DEFAULT_PAGINATION.page });
  const handlePageSizeChange = (newPage: number, newPerPage: number) => setPagination({ ...pagination, page: newPage, perPage: newPerPage });

  return (
    <Modal show>
      <Modal.Body>
        <PaginatedList onChange={handlePageSizeChange}
                       activePage={page}
                       totalItems={total}
                       pageSize={perPage}>
          <SearchForm focusAfterMount
                      onSearch={handleSearch}
                      topMargin={0}
                      onReset={() => handleSearch('')} />
          {loading && (<LoadingSpinner />)}
          <ListContainer>
            {!loading && total === 0 && (
              <NoSavedSearches>
                No saved searches found.
              </NoSavedSearches>
            )}
            {savedSearches?.length > 0 && (
              <StyledListGroup>
                {savedSearches.map((savedSearch) => (
                  <ViewLoaderContext.Consumer key={savedSearch.id}>
                    {(loaderFunc) => (
                      <ListGroupItem onClick={() => onLoad(toggleModal, savedSearch.id, loaderFunc)}
                                     header={savedSearch.title}
                                     active={savedSearch.id === activeSavedSearchId}>
                        {savedSearch.summary}
                        <DeleteButton onClick={(e) => onDelete(e, savedSearches, deleteSavedSearch, savedSearch.id)}
                                      role="button"
                                      title={`Delete search ${savedSearch.title}`}
                                      tabIndex={0}>
                          <Icon name="trash-alt" />
                        </DeleteButton>
                      </ListGroupItem>
                    )}
                  </ViewLoaderContext.Consumer>
                ))}
              </StyledListGroup>
            )}
          </ListContainer>
        </PaginatedList>
      </Modal.Body>
      <Modal.Footer>
        <Button onClick={toggleModal}>Cancel</Button>
      </Modal.Footer>
    </Modal>
  );
};

SavedSearchList.propTypes = {
  toggleModal: PropTypes.func.isRequired,
  deleteSavedSearch: PropTypes.func.isRequired,
};

export default SavedSearchList;
