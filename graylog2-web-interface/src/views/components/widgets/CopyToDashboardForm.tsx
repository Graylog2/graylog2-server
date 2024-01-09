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
import React, { useState, useCallback, useEffect, useRef } from 'react';

import { Modal, ListGroup, ListGroupItem, Input } from 'components/bootstrap';
import { PaginatedList, SearchForm, ModalSubmit, Spinner } from 'components/common';
import useDashboards from 'views/components/dashboard/hooks/useDashboards';
import type { SearchParams } from 'stores/PaginationTypes';

type Props = {
  activeDashboardId?: string,
  onCancel: () => void,
  onCopyToDashboard: (selectedDashboardId: string | undefined | null) => Promise<void>,
  submitButtonText: string,
  submitLoadingText: string,
  onCreateNewDashboard?: () => Promise<void>,
};

const CopyToDashboardForm = ({ onCancel, onCopyToDashboard, submitButtonText, submitLoadingText, activeDashboardId, onCreateNewDashboard }: Props) => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [selectedDashboard, setSelectedDashboard] = useState<string | null>(null);
  const isMounted = useRef<boolean>();
  const [createNewDashboard, setCreateNewDashboard] = useState(false);

  useEffect(() => {
    isMounted.current = true;

    return () => {
      isMounted.current = false;
    };
  }, []);

  const [searchParams, setSearchParams] = useState<SearchParams>({
    page: 1,
    pageSize: 5,
    query: '',
    sort: {
      attributeId: 'title',
      direction: 'asc',
    },
  });
  const { data: paginatedDashboards, isInitialLoading: isLoadingDashboards } = useDashboards(searchParams);

  useEffect(() => {
    setSelectedDashboard(null);
  }, [searchParams]);

  const handleSearch = useCallback(
    (newQuery: string) => setSearchParams((cur) => ({ ...cur, query: newQuery, page: 1 })),
    [],
  );
  const handleSearchReset = useCallback(() => handleSearch(''), [handleSearch]);
  const handlePageChange = useCallback((newPage: number, newPageSize: number) => setSearchParams(
    (cur) => ({ ...cur, page: newPage, pageSize: newPageSize })),
  [],
  );

  const handleSubmit = () => {
    setIsSubmitting(true);
    const submitHandler = async () => (createNewDashboard ? onCreateNewDashboard() : onCopyToDashboard(selectedDashboard));

    submitHandler().then(() => {
      if (isMounted.current) {
        setIsSubmitting(false);
      }
    });
  };

  const onSelectDashboard = (dashboardId: string) => {
    setSelectedDashboard(dashboardId);
    if (createNewDashboard) { setCreateNewDashboard(false); }
  };

  const toggleCreateNewDashboard = () => {
    setCreateNewDashboard((cur) => !cur);
    if (selectedDashboard) { setSelectedDashboard(null); }
  };

  const showCreateNewDashboardCheckbox = typeof onCreateNewDashboard === 'function';

  return (
    <Modal show onHide={() => {}}>
      <Modal.Body>
        {isLoadingDashboards && <Spinner />}
        {!isLoadingDashboards && (
          <>
            <PaginatedList onChange={handlePageChange}
                           activePage={searchParams.page}
                           totalItems={paginatedDashboards.pagination.total}
                           pageSize={searchParams.pageSize}
                           pageSizes={[5, 10, 15]}
                           useQueryParameter={false}>
              <div style={{ marginBottom: '5px' }}>
                <SearchForm onSearch={handleSearch}
                            onReset={handleSearchReset} />
              </div>
              {paginatedDashboards.list.length ? (
                <ListGroup>
                  {paginatedDashboards.list.map((dashboard) => {
                    const isActiveDashboard = activeDashboardId === dashboard.id;

                    return (
                      <ListGroupItem active={selectedDashboard === dashboard.id}
                                     onClick={isActiveDashboard ? undefined : () => onSelectDashboard(dashboard.id)}
                                     header={dashboard.title}
                                     disabled={isActiveDashboard}
                                     key={dashboard.id}>
                        {dashboard.summary}
                      </ListGroupItem>
                    );
                  })}
                </ListGroup>
              ) : <span>No dashboards found</span>}
            </PaginatedList>
            {showCreateNewDashboardCheckbox && (
              <Input type="checkbox"
                     id="create-new-dashboard"
                     name="createNewDashboard"
                     label="Create a new dashboard"
                     onChange={toggleCreateNewDashboard}
                     checked={createNewDashboard} />
            )}
          </>
        )}
      </Modal.Body>
      <Modal.Footer>
        <ModalSubmit submitButtonText={submitButtonText}
                     submitLoadingText={submitLoadingText}
                     isAsyncSubmit
                     isSubmitting={isSubmitting}
                     disabledSubmit={!selectedDashboard && !createNewDashboard}
                     submitButtonType="button"
                     onSubmit={handleSubmit}
                     onCancel={onCancel} />
      </Modal.Footer>
    </Modal>
  );
};

CopyToDashboardForm.defaultProps = {
  activeDashboardId: undefined,
  onCreateNewDashboard: undefined,
};

export default CopyToDashboardForm;
