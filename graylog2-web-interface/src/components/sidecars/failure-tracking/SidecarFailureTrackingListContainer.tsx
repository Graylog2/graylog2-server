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

import { Spinner } from 'components/common';
import { SidecarsActions } from 'stores/sidecars/SidecarsStore';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';

import SidecarFailureTrackingList from './SidecarFailureTrackingList';

import type { SidecarListResponse } from '../types';

type SidecarsArgs = {
  query?: string,
  onlyActive?: boolean,
  page?: number,
  pageSize?: number,
  sortField?: string,
  order?: 'asc'|'desc',
}

const SIDECARS_DEFAULT_ARGS: SidecarsArgs = {
  query: '',
  onlyActive: false,
  page: 1,
  pageSize: 10,
  sortField: 'node_name',
  order: 'asc',
};

const fetchSidecars = (options: SidecarsArgs = SIDECARS_DEFAULT_ARGS, callback: (data: SidecarListResponse) => void = () => {}) => {
  return SidecarsActions.listPaginated({
    query: options.query || SIDECARS_DEFAULT_ARGS.query,
    page: options.page || SIDECARS_DEFAULT_ARGS.page,
    pageSize: options.pageSize || SIDECARS_DEFAULT_ARGS.pageSize,
    onlyActive: options.onlyActive || SIDECARS_DEFAULT_ARGS.onlyActive,
    sortField: options.sortField || SIDECARS_DEFAULT_ARGS.sortField,
    order: options.order || SIDECARS_DEFAULT_ARGS.order,
  }).then(callback);
};

const SidecarFailureTrackingListContainer = () => {
  const { page, pageSize, resetPage } = usePaginationQueryParameter();
  const [sidecarData, setSidecarData] = useState<SidecarListResponse|null>(null);

  useEffect(() => {
    fetchSidecars({ page, pageSize }, setSidecarData);

    return () => {};
  }, [page, pageSize]);

  const handlePageChange = (_page: number, _pageSize: number) => {
    fetchSidecars({ page: _page, pageSize: _pageSize }, setSidecarData);
  };

  const handleQueryChange = (_query: string = '', callback = () => {}) => {
    resetPage();
    fetchSidecars({ query: _query }, setSidecarData).finally(callback);
  };

  const handleSortChange = (sortField: string) => {
    resetPage();
    fetchSidecars({ sortField, order: sidecarData.order === 'asc' ? 'desc' : 'asc' }, setSidecarData);
  };

  const toggleShowInactive = () => {
    resetPage();
    fetchSidecars({ onlyActive: !sidecarData.only_active }, setSidecarData);
  };

  const isLoading = !sidecarData;

  if (isLoading) {
    return <Spinner />;
  }

  return (
    <SidecarFailureTrackingList sidecars={sidecarData.sidecars}
                                pagination={sidecarData.pagination}
                                query={sidecarData.query}
                                onlyActive={sidecarData.only_active}
                                sort={{ field: sidecarData.sort, order: sidecarData.order }}
                                onPageChange={handlePageChange}
                                onQueryChange={handleQueryChange}
                                onSortChange={handleSortChange}
                                toggleShowInactive={toggleShowInactive} />
  );
};

export default SidecarFailureTrackingListContainer;
