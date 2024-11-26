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
import { useCallback, useEffect } from 'react';

import { Spinner } from 'components/common';
import withPaginationQueryParameter from 'components/common/withPaginationQueryParameter';
import type { PaginationOptions } from 'stores/sidecars/SidecarsStore';
import { SidecarsActions, SidecarsStore } from 'stores/sidecars/SidecarsStore';
import { useStore } from 'stores/connect';
import type { PaginationQueryParameterResult } from 'hooks/usePaginationQueryParameter';

import SidecarList, { PAGE_SIZES } from './SidecarList';

type Props = {
  paginationQueryParameter: PaginationQueryParameterResult,
}
const SIDECAR_DATA_REFRESH = 5 * 1000;

const SidecarListContainer = ({ paginationQueryParameter }: Props) => {
  const sidecars = useStore(SidecarsStore);

  const _reloadSidecars = useCallback(({ query, page, pageSize, onlyActive, sortField, order }: Partial<PaginationOptions> = {}) => {
    const effectiveQuery = query === undefined ? sidecars.query : query;

    const options: Partial<PaginationOptions> = {
      query: effectiveQuery,
      onlyActive: 'true',
    };

    if (sidecars.sort) {
      options.sortField = sortField || sidecars.sort.field;
      options.order = order || sidecars.sort.order;
    }

    options.pageSize = pageSize || paginationQueryParameter.pageSize;
    options.onlyActive = onlyActive === undefined ? sidecars.onlyActive : onlyActive; // Avoid || to handle false values

    const shouldKeepPage = options.pageSize === paginationQueryParameter.pageSize
        && options.onlyActive === sidecars.onlyActive
        && options.query === sidecars.query; // Only keep page number when other parameters don't change
    let effectivePage = 1;

    if (shouldKeepPage) {
      effectivePage = page || paginationQueryParameter.page;
    }

    options.page = effectivePage;

    return SidecarsActions.listPaginated(options);
  }, [paginationQueryParameter.page, paginationQueryParameter.pageSize, sidecars.onlyActive, sidecars.query, sidecars.sort]);

  useEffect(() => {
    _reloadSidecars();
    const interval = setInterval(() => _reloadSidecars({}), SIDECAR_DATA_REFRESH);

    return () => clearInterval(interval);
  }, [_reloadSidecars]);

  const handleSortChange = useCallback((field) => () => {
    _reloadSidecars({
      sortField: field,
      // eslint-disable-next-line no-nested-ternary
      order: (sidecars.sort.field === field ? (sidecars.sort.order === 'asc' ? 'desc' : 'asc') : 'asc'),
    });
  }, [_reloadSidecars, sidecars.sort.field, sidecars.sort.order]);

  const handlePageChange = useCallback((page, pageSize) => {
    _reloadSidecars({ page: page, pageSize: pageSize });
  }, [_reloadSidecars]);

  const handleQueryChange = useCallback((query = '', callback = () => {}) => {
    const { resetPage } = paginationQueryParameter;

    resetPage();

    _reloadSidecars({ query: query }).finally(callback);
  }, [_reloadSidecars, paginationQueryParameter]);

  const toggleShowInactive = useCallback(() => {
    const { resetPage } = paginationQueryParameter;

    resetPage();

    _reloadSidecars({ onlyActive: !sidecars.onlyActive });
  }, [_reloadSidecars, paginationQueryParameter, sidecars.onlyActive]);

  const { sidecars: sidecarsList, onlyActive, pagination, query, sort } = sidecars;

  const isLoading = !sidecarsList;

  if (isLoading) {
    return <Spinner />;
  }

  return (
    <SidecarList sidecars={sidecarsList}
                 onlyActive={onlyActive}
                 pagination={pagination}
                 query={query}
                 sort={sort}
                 onPageChange={handlePageChange}
                 onQueryChange={handleQueryChange}
                 onSortChange={handleSortChange}
                 toggleShowInactive={toggleShowInactive} />
  );
};

export default withPaginationQueryParameter(SidecarListContainer, { pageSizes: PAGE_SIZES });
