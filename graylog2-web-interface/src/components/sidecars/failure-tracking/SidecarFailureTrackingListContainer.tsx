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
import { useState } from 'react';

import { Spinner } from 'components/common';
import { useSidecarsListPaginated } from 'hooks/useSidecars';
import { useCollectorsAll } from 'hooks/useCollectors';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';

import SidecarFailureTrackingList from './SidecarFailureTrackingList';

const PAGE_SIZES = [25];

const SIDECARS_DEFAULTS = {
  query: '',
  sortField: 'last_seen',
  order: 'desc',
  onlyActive: false as boolean,
};

const SidecarFailureTrackingListContainer = () => {
  const { page, pageSize, resetPage, setPagination } = usePaginationQueryParameter(PAGE_SIZES);
  const [query, setQuery] = useState<string>(SIDECARS_DEFAULTS.query);
  const [sortField, setSortField] = useState<string>(SIDECARS_DEFAULTS.sortField);
  const [order, setOrder] = useState<string>(SIDECARS_DEFAULTS.order);
  const [onlyActive, setOnlyActive] = useState<boolean>(SIDECARS_DEFAULTS.onlyActive);
  const { data: collectors = [] } = useCollectorsAll();

  const { data: sidecarData } = useSidecarsListPaginated({
    query,
    page,
    pageSize,
    sortField,
    order,
    onlyActive,
  });

  const handlePageChange = (newPage: number, newPageSize: number) => {
    setPagination({ page: newPage, pageSize: newPageSize });
  };

  const handleQueryChange = (_query: string = '', callback: () => void = () => {}) => {
    setQuery(_query);
    resetPage();
    callback();
  };

  const handleSortChange = (newSortField: string) => {
    setSortField(newSortField);
    setOrder((prev) => (prev === 'asc' ? 'desc' : 'asc'));
    resetPage();
  };

  const toggleShowInactive = () => {
    setOnlyActive((prev) => !prev);
    resetPage();
  };

  const isLoading = !sidecarData;

  if (isLoading) {
    return <Spinner />;
  }

  return (
    <SidecarFailureTrackingList
      sidecars={sidecarData.sidecars}
      collectors={collectors}
      pagination={{
        total: sidecarData.pagination.total,
        count: sidecarData.pagination.count,
        page: sidecarData.pagination.page,
        per_page: sidecarData.pagination.pageSize,
      }}
      query={sidecarData.query}
      onlyActive={Boolean(sidecarData.onlyActive)}
      sort={{ field: sidecarData.sort.field, order: sidecarData.sort.order }}
      onPageChange={handlePageChange}
      onQueryChange={handleQueryChange}
      onSortChange={handleSortChange}
      toggleShowInactive={toggleShowInactive}
    />
  );
};

export default SidecarFailureTrackingListContainer;
