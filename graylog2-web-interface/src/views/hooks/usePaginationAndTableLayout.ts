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
import { useCallback, useMemo, useState } from 'react';

import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import type { Sort } from 'stores/PaginationTypes';

type DefaultLayout = {
  pageSize: number,
  sort: Sort,
  displayedColumns: Array<string>,
  columnsOrder: Array<string>,
}

const usePaginationAndTableLayout = (entityTableId: string, defaultLayout: DefaultLayout) => {
  const [query, setQuery] = useState('');
  const [activePage, setActivePage] = useState(1);
  const { layoutConfig, isInitialLoading: isLoadingLayoutPreferences } = useTableLayout({
    entityTableId: entityTableId,
    defaultPageSize: defaultLayout.pageSize,
    defaultDisplayedAttributes: defaultLayout.displayedColumns,
    defaultSort: defaultLayout.sort,
  });
  const { mutate: updateTableLayout } = useUpdateUserLayoutPreferences(entityTableId);
  const searchParams = useMemo(() => ({
    query,
    page: activePage,
    pageSize: layoutConfig.pageSize,
    sort: layoutConfig.sort,
  }), [activePage, layoutConfig.pageSize, layoutConfig.sort, query]);

  const onPageChange = useCallback(
    (newPage: number, newPageSize: number) => {
      if (newPage) {
        setActivePage(newPage);
      }

      if (newPageSize) {
        updateTableLayout({ perPage: newPageSize });
      }
    }, [updateTableLayout],
  );

  const onPageSizeChange = useCallback((newPageSize: number) => {
    setActivePage(1);
    updateTableLayout({ perPage: newPageSize });
  }, [updateTableLayout]);

  const onSortChange = useCallback((newSort: Sort) => {
    setActivePage(1);
    updateTableLayout({ sort: newSort });
  }, [updateTableLayout]);

  const onSearch = useCallback((newQuery: string) => {
    setActivePage(1);
    setQuery(newQuery);
  }, []);

  const onResetSearch = useCallback(() => onSearch(''), [onSearch]);

  const onColumnsChange = useCallback((displayedAttributes: Array<string>) => {
    updateTableLayout({ displayedAttributes });
  }, [updateTableLayout]);

  return ({
    activePage,
    isLoadingLayoutPreferences,
    onPageChange,
    layoutConfig,
    onSearch,
    onResetSearch,
    searchParams,
    onColumnsChange,
    onSortChange,
    onPageSizeChange,
    columnsOrder: defaultLayout.columnsOrder,
  });
};

export default usePaginationAndTableLayout;
