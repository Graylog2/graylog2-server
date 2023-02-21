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
import { CollectorsActions } from 'stores/sidecars/CollectorsStore';

import SidecarFailureTrackingList from './SidecarFailureTrackingList';

import type { Collector, SidecarListResponse } from '../types';

type SidecarsArgs = {
  page?: number,
  pageSize?: number,
  query?: string,
  sortField?: string,
  order?: string,
  onlyActive?: boolean,
}

const PAGE_SIZES = [25];

const SIDECARS_DEFAULT_ARGS: SidecarsArgs = {
  page: 1,
  pageSize: PAGE_SIZES[0],
  query: '',
  sortField: 'last_seen',
  order: 'desc',
  onlyActive: false,
};

const fetchSidecars = (options: SidecarsArgs, callback: (data: SidecarListResponse) => void) => {
  const {
    page = SIDECARS_DEFAULT_ARGS.page,
    pageSize = SIDECARS_DEFAULT_ARGS.pageSize,
    query = SIDECARS_DEFAULT_ARGS.query,
    sortField = SIDECARS_DEFAULT_ARGS.sortField,
    order = SIDECARS_DEFAULT_ARGS.order,
    onlyActive = SIDECARS_DEFAULT_ARGS.onlyActive,
  } = options;

  return SidecarsActions.listPaginated({
    page,
    pageSize,
    query,
    sortField,
    order,
    onlyActive,
  }).then(callback);
};

const SidecarFailureTrackingListContainer = () => {
  const { page, pageSize, resetPage } = usePaginationQueryParameter(PAGE_SIZES);
  const [sidecarData, setSidecarData] = useState<SidecarListResponse|null>(null);
  const [collectors, setCollectors] = useState<Collector[]>([]);

  useEffect(() => {
    if (sidecarData?.pagination.page !== page || sidecarData?.pagination.per_page !== pageSize) {
      const { query, sort, order, only_active } = sidecarData || {};
      fetchSidecars({ query, page, pageSize, order, sortField: sort, onlyActive: only_active }, setSidecarData);
    }
  }, [page, pageSize, sidecarData]);

  useEffect(() => {
    CollectorsActions.all().then((response) => setCollectors(response.collectors));
  }, []);

  const previousSidecarArgs: SidecarsArgs = {
    page: 1,
    pageSize,
    query: sidecarData?.query,
    sortField: sidecarData?.sort,
    order: sidecarData?.order,
    onlyActive: sidecarData?.only_active,
  };

  const handlePageChange = (_page: number, _pageSize: number) => {
    fetchSidecars({ ...previousSidecarArgs, page: _page, pageSize: _pageSize }, setSidecarData);
  };

  const handleQueryChange = (_query: string = '', callback = () => {}) => {
    fetchSidecars({ ...previousSidecarArgs, query: _query }, setSidecarData).then(resetPage).finally(callback);
  };

  const handleSortChange = (sortField: string) => {
    fetchSidecars({ ...previousSidecarArgs, sortField, order: sidecarData.order === 'asc' ? 'desc' : 'asc' }, setSidecarData).then(resetPage);
  };

  const toggleShowInactive = () => {
    fetchSidecars({ ...previousSidecarArgs, onlyActive: !sidecarData.only_active }, setSidecarData).then(resetPage);
  };

  const isLoading = !sidecarData;

  if (isLoading) {
    return <Spinner />;
  }

  return (
    <SidecarFailureTrackingList sidecars={sidecarData.sidecars}
                                collectors={collectors}
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
