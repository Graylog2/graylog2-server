import * as React from 'react';
import { useMemo } from 'react';
import { useQueryParam, StringParam } from 'use-query-params';
import { useQuery } from '@tanstack/react-query';

import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import { useTableEventHandlers } from 'components/common/EntityDataTable';
import { Spinner, PaginatedList, SearchForm, NoSearchResult, EntityDataTable } from 'components/common';
import type { Attribute, SearchParams } from 'stores/PaginationTypes';
import type { EntityBase } from 'components/common/EntityDataTable/types';
import UserNotification from 'util/UserNotification';

type PaginatedResponse<T> = {
  list: Array<T>,
  pagination: {
    total: number
  },
  attributes: Array<Attribute>,
}

type Props<T> = {
  actionsCellWidth?: React.ComponentProps<typeof EntityDataTable>['actionsCellWidth'],
  columnRenderers: React.ComponentProps<typeof EntityDataTable>['columnRenderers'],
  columnsOrder: React.ComponentProps<typeof EntityDataTable>['columnsOrder'],
  entityActions: React.ComponentProps<typeof EntityDataTable>['rowActions'],
  fetchData: (options: SearchParams) => Promise<PaginatedResponse<T>>,
  humanName: string,
  keyFn: (options: SearchParams) => Array<unknown>,
  queryHelpComponent?: React.ReactNode,
  tableLayout: Parameters<typeof useTableLayout>[0],
}

const INITIAL_DATA = {
  pagination: { total: 0 },
  list: [],
  attributes: [],
};

/*
 * This component is a wrapper for the EntityDataTable.
 * It should be used whenever you want to render the table on a page.
 * It contains all the required logic to e.g. sync the URL query params.
 * It should not be used when there are multiple entity tables on the page or when the table is rendered in a modal.
 */

const PageEntityTable = <T extends EntityBase>({
  actionsCellWidth, columnsOrder, entityActions, tableLayout, fetchData, keyFn,
  humanName, columnRenderers, queryHelpComponent,
}: Props<T>) => {
  const [query, setQuery] = useQueryParam('query', StringParam);
  const { layoutConfig, isInitialLoading: isLoadingLayoutPreferences } = useTableLayout(tableLayout);
  const paginationQueryParameter = usePaginationQueryParameter(undefined, layoutConfig.pageSize, false);
  const { mutate: updateTableLayout } = useUpdateUserLayoutPreferences(tableLayout.entityTableId);
  const fetchOptions = useMemo(() => ({
    query,
    page: paginationQueryParameter.page,
    pageSize: layoutConfig.pageSize,
    sort: layoutConfig.sort,
  }), [layoutConfig.pageSize, layoutConfig.sort, paginationQueryParameter.page, query]);
  const fetchKey = useMemo(() => keyFn(fetchOptions), [fetchOptions, keyFn]);
  const { data: paginatedReports = INITIAL_DATA, isInitialLoading: isLoadingReports } = useQuery(
    fetchKey,
    () => fetchData(fetchOptions),
    {
      enabled: !isLoadingLayoutPreferences,
      onError: (error) => {
        UserNotification.error(`Fetching ${humanName} failed with status: ${error}`, `Could not retrieve ${humanName}`);
      },
      keepPreviousData: true,
    },
  );

  const {
    onPageSizeChange,
    onSearch,
    onSearchReset,
    onColumnsChange,
    onSortChange,
  } = useTableEventHandlers({
    appSection: `${tableLayout.entityTableId}-list`,
    paginationQueryParameter,
    updateTableLayout,
    setQuery,
  });

  const columnDefinitions = useMemo(
    () => ([...(paginatedReports?.attributes ?? [])]),
    [paginatedReports?.attributes],
  );

  if (isLoadingLayoutPreferences || isLoadingReports) {
    return <Spinner />;
  }

  const { list, pagination: { total } } = paginatedReports;

  return (
    <PaginatedList pageSize={layoutConfig.pageSize}
                   showPageSizeSelect={false}
                   totalItems={total}>
      <div style={{ marginBottom: 5 }}>
        <SearchForm onSearch={onSearch}
                    onReset={onSearchReset}
                    query={query}
                    queryHelpComponent={queryHelpComponent} />
      </div>
      <div>
        {list?.length === 0 ? (
          <NoSearchResult>No {humanName} have been found</NoSearchResult>
        ) : (
          <EntityDataTable<T> data={list}
                              visibleColumns={layoutConfig.displayedAttributes}
                              columnsOrder={columnsOrder}
                              onColumnsChange={onColumnsChange}
                              onSortChange={onSortChange}
                              onPageSizeChange={onPageSizeChange}
                              pageSize={layoutConfig.pageSize}
                              activeSort={layoutConfig.sort}
                              rowActions={entityActions}
                              actionsCellWidth={actionsCellWidth}
                              columnRenderers={columnRenderers}
                              columnDefinitions={columnDefinitions}
                              entityAttributesAreCamelCase={false} />
        )}
      </div>
    </PaginatedList>
  );
};

PageEntityTable.defaultProps = {
  actionsCellWidth: 160,
  queryHelpComponent: undefined,
};

export default PageEntityTable;
