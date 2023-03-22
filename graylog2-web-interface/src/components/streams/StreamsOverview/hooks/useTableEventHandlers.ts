import { useCallback } from 'react';

import type { Sort } from 'stores/PaginationTypes';

const useTableEventHandlers = ({ updateTableLayout, paginationQueryParameter, setQuery }: { updateTableLayout, paginationQueryParameter, setQuery }) => {
  const onPageSizeChange = useCallback((newPageSize: number) => {
    paginationQueryParameter.setPagination({ page: 1, pageSize: newPageSize });
    updateTableLayout({ perPage: newPageSize });
  }, [paginationQueryParameter, updateTableLayout]);

  const onSearch = useCallback((newQuery: string) => {
    paginationQueryParameter.resetPage();
    setQuery(newQuery);
  }, [paginationQueryParameter]);

  const onSearchReset = useCallback(() => {
    onSearch('');
  }, [onSearch]);

  const onColumnsChange = useCallback((displayedAttributes: Array<string>) => {
    updateTableLayout({ displayedAttributes });
  }, [updateTableLayout]);

  const onSortChange = useCallback((newSort: Sort) => {
    paginationQueryParameter.resetPage();
    updateTableLayout({ sort: newSort });
  }, [paginationQueryParameter, updateTableLayout]);

  return {
    onPageSizeChange,
    onSearch,
    onSearchReset,
    onColumnsChange,
    onSortChange,
  };
};

export default useTableEventHandlers;
