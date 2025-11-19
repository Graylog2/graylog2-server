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
import { useCallback, useMemo } from 'react';

import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import type { ColumnPreferences, DefaultLayout } from 'components/common/EntityDataTable/types';
import type { SearchParams, Sort } from 'stores/PaginationTypes';

import { DEFAULT_VISIBLE_COLUMNS } from './DataNodesColumnConfiguration';

const TABLE_LAYOUT: DefaultLayout = {
  entityTableId: 'cluster-data-nodes',
  defaultSort: { attributeId: 'hostname', direction: 'asc' },
  defaultDisplayedAttributes: [...DEFAULT_VISIBLE_COLUMNS],
  defaultPageSize: 0,
};

const DEFAULT_SEARCH_PARAMS: SearchParams = {
  query: '',
  page: 1,
  pageSize: 0,
  sort: { attributeId: 'hostname', direction: 'asc' },
};

type UseClusterDataNodesTableLayoutReturn = {
  columnsOrder: Array<string>;
  columnPreferences?: ColumnPreferences;
  defaultDisplayedColumns: Array<string>;
  searchParams: SearchParams;
  isLoadingLayout: boolean;
  handleColumnPreferencesChange: (newColumnPreferences: ColumnPreferences) => void;
  handleSortChange: (newSort: Sort) => void;
};

const useClusterDataNodesTableLayout = (
  searchQuery = '',
  pageSizeLimit?: number,
): UseClusterDataNodesTableLayoutReturn => {
  const columnsOrder = useMemo<Array<string>>(() => [...DEFAULT_VISIBLE_COLUMNS], []);
  const { layoutConfig, isInitialLoading: isLoadingLayout } = useTableLayout(TABLE_LAYOUT);
  const { mutate: updateTableLayout } = useUpdateUserLayoutPreferences(TABLE_LAYOUT.entityTableId);

  const handleColumnPreferencesChange = useCallback(
    (newColumnPreferences: ColumnPreferences) => {
      updateTableLayout({ attributes: newColumnPreferences });
    },
    [updateTableLayout],
  );

  const handleSortChange = useCallback(
    (newSort: Sort) => {
      updateTableLayout({ sort: newSort });
    },
    [updateTableLayout],
  );

  const searchParams = useMemo<SearchParams>(
    () => ({
      ...DEFAULT_SEARCH_PARAMS,
      query: searchQuery,
      sort: layoutConfig.sort,
      pageSize: pageSizeLimit ?? layoutConfig.pageSize ?? DEFAULT_SEARCH_PARAMS.pageSize,
    }),
    [layoutConfig.pageSize, layoutConfig.sort, pageSizeLimit, searchQuery],
  );

  return {
    columnsOrder,
    columnPreferences: layoutConfig.columnPreferences,
    defaultDisplayedColumns: layoutConfig.defaultDisplayedColumns,
    searchParams,
    isLoadingLayout,
    handleColumnPreferencesChange,
    handleSortChange,
  };
};

export default useClusterDataNodesTableLayout;
