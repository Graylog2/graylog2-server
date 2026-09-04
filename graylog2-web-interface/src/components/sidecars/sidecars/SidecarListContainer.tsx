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
import { useCallback, useState } from 'react';

import { Spinner } from 'components/common';
import withPaginationQueryParameter from 'components/common/withPaginationQueryParameter';
import type { PaginationOptions } from 'hooks/useSidecars';
import { useSidecarsListPaginated } from 'hooks/useSidecars';
import type { PaginationQueryParameterResult } from 'hooks/usePaginationQueryParameter';

import SidecarList, { PAGE_SIZES } from './SidecarList';

type Props = {
  paginationQueryParameter: PaginationQueryParameterResult;
};

const SidecarListContainer = ({ paginationQueryParameter }: Props) => {
  const [query, setQuery] = useState<string>('');
  const [onlyActive, setOnlyActive] = useState<string | boolean>('true');
  const [sortField, setSortField] = useState<string>('node_name');
  const [order, setOrder] = useState<string>('asc');

  const queryOptions: Partial<PaginationOptions> = {
    query,
    onlyActive,
    sortField,
    order,
    page: paginationQueryParameter.page,
    pageSize: paginationQueryParameter.pageSize,
  };

  const { data, isLoading } = useSidecarsListPaginated(queryOptions);

  const handleSortChange = useCallback(
    (field: string) => () => {
      let nextOrder = 'asc';

      if (sortField === field) {
        nextOrder = order === 'asc' ? 'desc' : 'asc';
      }

      setSortField(field);
      setOrder(nextOrder);
    },
    [sortField, order],
  );

  const handlePageChange = useCallback(
    (page: number, pageSize: number) => {
      paginationQueryParameter.setPagination({ page, pageSize });
    },
    [paginationQueryParameter],
  );

  const handleQueryChange = useCallback(
    (newQuery: string = '', callback: () => void = () => {}) => {
      const { resetPage } = paginationQueryParameter;

      resetPage();
      setQuery(newQuery);
      callback();

      return Promise.resolve();
    },
    [paginationQueryParameter],
  );

  const toggleShowInactive = useCallback(() => {
    const { resetPage } = paginationQueryParameter;

    resetPage();
    setOnlyActive((prev) => !prev);
  }, [paginationQueryParameter]);

  if (isLoading || !data) {
    return <Spinner />;
  }

  return (
    <SidecarList
      sidecars={data.sidecars}
      onlyActive={data.onlyActive}
      pagination={data.pagination}
      query={data.query}
      sort={data.sort}
      onPageChange={handlePageChange}
      onQueryChange={handleQueryChange}
      onSortChange={handleSortChange}
      toggleShowInactive={toggleShowInactive}
    />
  );
};

export default withPaginationQueryParameter(SidecarListContainer, { pageSizes: PAGE_SIZES });
