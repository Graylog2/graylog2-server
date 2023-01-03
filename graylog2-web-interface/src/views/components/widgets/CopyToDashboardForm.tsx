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
import React, { useState, useCallback, useEffect } from 'react';

import { Modal, ListGroup, ListGroupItem } from 'components/bootstrap';
import { PaginatedList, SearchForm, ModalSubmit } from 'components/common';
import useDashboards from 'views/components/dashboard/hooks/useDashboards';
import type { SearchParams } from 'stores/PaginationTypes';

type Props = {
  onCancel: () => void,
  onSubmit: (selectedDashboardId: string | undefined | null) => Promise<void>,
  submitButtonText: string,
  submittingText: string,
};

const CopyToDashboardForm = ({ onCancel, onSubmit, submitButtonText, submittingText }: Props) => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [selectedDashboard, setSelectedDashboard] = useState<string | null>(null);
  const [searchParams, setSearchParams] = useState<SearchParams>({
    page: 1,
    pageSize: 5,
    query: '',
    sort: {
      attributeId: 'title',
      direction: 'asc',
    },
  });
  const { data: paginatedDashboards } = useDashboards(searchParams);

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

    onSubmit(selectedDashboard).finally(() => {
      setIsSubmitting(false);
    });
  };

  return (
    <Modal show>
      <Modal.Body>
        <SearchForm onSearch={handleSearch}
                    onReset={handleSearchReset} />
        <PaginatedList onChange={handlePageChange}
                       activePage={searchParams.page}
                       totalItems={paginatedDashboards?.pagination?.total ?? 0}
                       pageSize={searchParams.pageSize}
                       pageSizes={[5, 10, 15]}
                       useQueryParameter={false}>
          {paginatedDashboards?.list && paginatedDashboards.list.length > 0 ? (
            <ListGroup>
              {paginatedDashboards.list.map((dashboard) => (
                <ListGroupItem active={selectedDashboard === dashboard.id}
                               onClick={() => setSelectedDashboard(dashboard.id)}
                               header={dashboard.title}
                               key={dashboard.id}>
                  {dashboard.summary}
                </ListGroupItem>
              ))}
            </ListGroup>
          ) : <span>No dashboards found</span>}
        </PaginatedList>
      </Modal.Body>
      <Modal.Footer>
        <ModalSubmit submitButtonText={submitButtonText}
                     submitLoadingText={submittingText}
                     isAsyncSubmit
                     isSubmitting={isSubmitting}
                     disabledSubmit={!selectedDashboard}
                     submitButtonType="button"
                     onSubmit={handleSubmit}
                     onCancel={onCancel} />
      </Modal.Footer>
    </Modal>
  );
};

export default CopyToDashboardForm;
