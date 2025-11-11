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
import { useCallback, useEffect, useMemo, useState } from 'react';
import isEqual from 'lodash/isEqual';

import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import type { DefaultLayout } from 'components/common/EntityDataTable/types';
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
  visibleColumns: Array<string>;
  searchParams: SearchParams;
  isLoadingLayout: boolean;
  handleColumnsChange: (newColumns: Array<string>) => void;
  handleSortChange: (newSort: Sort) => void;
};

const useClusterDataNodesTableLayout = (): UseClusterDataNodesTableLayoutReturn => {
  const columnsOrder = useMemo<Array<string>>(() => [...DEFAULT_VISIBLE_COLUMNS], []);
  const { layoutConfig, isInitialLoading: isLoadingLayout } = useTableLayout(TABLE_LAYOUT);
  const { mutate: updateTableLayout } = useUpdateUserLayoutPreferences(TABLE_LAYOUT.entityTableId);
  const [visibleColumns, setVisibleColumns] = useState<Array<string>>(layoutConfig.displayedAttributes);
  const [searchParams, setSearchParams] = useState<SearchParams>({
    ...DEFAULT_SEARCH_PARAMS,
    sort: layoutConfig.sort,
  });

  useEffect(() => {
    if (!isLoadingLayout && !isEqual(visibleColumns, layoutConfig.displayedAttributes)) {
      setVisibleColumns(layoutConfig.displayedAttributes);
    }
  }, [isLoadingLayout, layoutConfig.displayedAttributes, visibleColumns]);

  useEffect(() => {
    if (!isLoadingLayout && !isEqual(searchParams.sort, layoutConfig.sort)) {
      setSearchParams((prev) => ({
        ...prev,
        sort: layoutConfig.sort,
      }));
    }
  }, [isLoadingLayout, layoutConfig.sort, searchParams.sort]);

  const handleColumnsChange = useCallback(
    (newColumns: Array<string>) => {
      if (!newColumns.length) {
        return;
      }

      setVisibleColumns(newColumns);
      updateTableLayout({ displayedAttributes: newColumns });
    },
    [updateTableLayout],
  );

  const handleSortChange = useCallback(
    (newSort: Sort) => {
      setSearchParams((prev) => ({
        ...prev,
        sort: newSort,
      }));
      updateTableLayout({ sort: newSort });
    },
    [updateTableLayout],
  );

  return {
    columnsOrder,
    visibleColumns,
    searchParams,
    isLoadingLayout,
    handleColumnsChange,
    handleSortChange,
  };
};

export default useClusterDataNodesTableLayout;
