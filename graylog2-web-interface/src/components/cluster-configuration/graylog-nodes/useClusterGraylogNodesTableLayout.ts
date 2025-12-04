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

import useTableLayout, { type LayoutConfig } from 'components/common/EntityDataTable/hooks/useTableLayout';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import type { ColumnPreferences, DefaultLayout } from 'components/common/EntityDataTable/types';
import type { SearchParams, Sort } from 'stores/PaginationTypes';

import { DEFAULT_VISIBLE_COLUMNS } from './GraylogNodesColumnConfiguration';

const TABLE_LAYOUT: DefaultLayout = {
  entityTableId: 'cluster-graylog-nodes',
  defaultSort: { attributeId: 'hostname', direction: 'asc' },
  defaultDisplayedAttributes: [...DEFAULT_VISIBLE_COLUMNS],
  defaultPageSize: 0,
  defaultColumnOrder: [...DEFAULT_VISIBLE_COLUMNS],
};

const DEFAULT_SEARCH_PARAMS: SearchParams = {
  query: '',
  page: 1,
  pageSize: 0,
  sort: { attributeId: 'hostname', direction: 'asc' },
};

type UseClusterGraylogNodesTableLayoutReturn = {
  defaultDisplayedColumns: Array<string>;
  defaultColumnOrder: Array<string>;
  layoutPreferences: LayoutConfig;
  searchParams: SearchParams;
  isLoadingLayout: boolean;
  handleLayoutPreferencesChange: (newLayoutPreferences: {
    attributes?: ColumnPreferences;
    order?: Array<string>;
  }) => Promise<void>;
  handleSortChange: (newSort: Sort) => void;
  resetLayoutPreferences: () => Promise<void>;
};

const useClusterGraylogNodesTableLayout = (
  searchQuery = '',
  pageSizeLimit?: number,
): UseClusterGraylogNodesTableLayoutReturn => {
  const { layoutConfig, isInitialLoading: isLoadingLayout } = useTableLayout(TABLE_LAYOUT);
  const { mutateAsync: updateTableLayout } = useUpdateUserLayoutPreferences(TABLE_LAYOUT.entityTableId);

  const handleLayoutPreferencesChange = useCallback(
    (layoutPreferences: { attributes?: ColumnPreferences; order?: Array<string> }) => {
      const newLayoutPreferences: { attributes?: ColumnPreferences; order?: Array<string> } = {};

      if ('order' in layoutPreferences) {
        newLayoutPreferences.order = layoutPreferences.order;
      }

      if ('attributes' in layoutPreferences) {
        newLayoutPreferences.attributes = layoutPreferences.attributes;
      }

      return updateTableLayout(newLayoutPreferences);
    },
    [updateTableLayout],
  );

  const resetLayoutPreferences = useCallback(
    () => updateTableLayout({ attributes: null, order: null, sort: undefined, perPage: undefined }),
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
    defaultDisplayedColumns: TABLE_LAYOUT.defaultDisplayedAttributes,
    defaultColumnOrder: TABLE_LAYOUT.defaultColumnOrder,
    layoutPreferences: layoutConfig,
    resetLayoutPreferences,
    searchParams,
    isLoadingLayout,
    handleLayoutPreferencesChange,
    handleSortChange,
  };
};

export default useClusterGraylogNodesTableLayout;
