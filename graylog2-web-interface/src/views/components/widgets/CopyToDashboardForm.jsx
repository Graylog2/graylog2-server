// @flow strict
import React, { useEffect, useState, useCallback } from 'react';
import { Modal, Button, ListGroup, ListGroupItem } from 'components/graylog';
import type { DashboardsStoreState } from 'views/stores/DashboardsStore';
import connect from 'stores/connect';
import { PaginatedList, SearchForm } from 'components/common';
import { DashboardsActions, DashboardsStore } from 'views/stores/DashboardsStore';

type Props = {
  onCancel: () => void,
  onSubmit: (string, ?string) => void,
  widgetId: string,
  dashboards: DashboardsStoreState,
};

const CopyToDashboardForm = ({ widgetId, onCancel, dashboards: { list = [], pagination }, onSubmit }: Props) => {
  const [selectedDashboard, setSelectedDashboard] = useState<?string>(null);
  const [paginationState, setPaginationState] = useState({ query: '', page: 1, perPage: 5 });

  const handleSearch = useCallback((query) => {
    setPaginationState({
      ...paginationState,
      query,
    });
    setSelectedDashboard(null);
  }, [paginationState, setSelectedDashboard, setPaginationState]);

  const handleSearchReset = useCallback(() => handleSearch(''), [handleSearch]);

  const handlePageChange = useCallback((page: number, perPage: number) => {
    setPaginationState({
      ...paginationState,
      page,
      perPage,
    });
    setSelectedDashboard(null);
  }, [paginationState, setSelectedDashboard, setPaginationState]);

  useEffect(() => {
    DashboardsActions.search(paginationState.query, paginationState.page, paginationState.perPage);
  }, [paginationState]);

  const dashboardList = list.map((dashboard) => {
    return (
      <ListGroupItem active={selectedDashboard === dashboard.id}
                     onClick={() => setSelectedDashboard(dashboard.id)}
                     header={dashboard.title}
                     key={dashboard.id}>
        {dashboard.summary}
      </ListGroupItem>
    );
  });

  const renderResult = list && list.length > 0
    ? <ListGroup>{dashboardList}</ListGroup>
    : <span>No dashboards found</span>;

  return (
    <Modal show>
      <Modal.Body>
        <SearchForm onSearch={handleSearch}
                    onReset={handleSearchReset} />
        <PaginatedList onChange={handlePageChange}
                       activePage={paginationState.page}
                       totalItems={pagination.total}
                       pageSize={paginationState.perPage}
                       pageSizes={[5, 10, 15]}>
          {renderResult}
        </PaginatedList>
      </Modal.Body>
      <Modal.Footer>
        <Button bsStyle="primary"
                disabled={selectedDashboard === null}
                onClick={() => onSubmit(widgetId, selectedDashboard)}>
          Select
        </Button>
        <Button onClick={onCancel}>Cancel</Button>
      </Modal.Footer>
    </Modal>
  );
};

export default connect(CopyToDashboardForm, { dashboards: DashboardsStore });
