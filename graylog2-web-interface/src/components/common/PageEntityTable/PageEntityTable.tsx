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
import { useMemo, useCallback } from 'react';
import { useQueryParam, StringParam } from 'use-query-params';
import styled from 'styled-components';

import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import { useTableEventHandlers } from 'components/common/EntityDataTable';
import { Spinner, PaginatedList, SearchForm, NoSearchResult, EntityDataTable } from 'components/common';
import type { Attribute, SearchParams } from 'stores/PaginationTypes';
import type { EntityBase } from 'components/common/EntityDataTable/types';
import EntityFilters from 'components/common/EntityFilters';
import useUrlQueryFilters from 'components/common/EntityFilters/hooks/useUrlQueryFilters';
import type { UrlQueryFilters } from 'components/common/EntityFilters/types';
import TableFetchContextProvider from 'components/common/PageEntityTable/TableFetchContextProvider';
import type { PaginatedResponse } from 'components/common/PageEntityTable/useFetchEntities';
import useFetchEntities from 'components/common/PageEntityTable/useFetchEntities';

const SearchRow = styled.div`
  margin-bottom: 5px;
  display: flex;
  width: 100%;
  justify-content: space-between;
  align-items: center;
`;

type Props<T> = {
  actionsCellWidth?: React.ComponentProps<typeof EntityDataTable>['actionsCellWidth'],
  bulkSelection?: React.ComponentProps<typeof EntityDataTable>['bulkSelection'],
  columnRenderers: React.ComponentProps<typeof EntityDataTable>['columnRenderers'],
  columnsOrder: React.ComponentProps<typeof EntityDataTable>['columnsOrder'],
  entityActions: React.ComponentProps<typeof EntityDataTable>['rowActions'],
  expandedSectionsRenderer?: React.ComponentProps<typeof EntityDataTable>['expandedSectionsRenderer'],
  fetchData: (options: SearchParams) => Promise<PaginatedResponse<T>>,
  filterValueRenderers?: React.ComponentProps<typeof EntityFilters>['filterValueRenderers'],
  humanName: string,
  keyFn: (options: SearchParams) => Array<unknown>,
  queryHelpComponent?: React.ReactNode,
  tableLayout: Parameters<typeof useTableLayout>[0],
  additionalAttributes?: Array<Attribute>,
  entityAttributesAreCamelCase?: boolean,
  topRightCol?: React.ReactNode,
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
  humanName, columnRenderers, queryHelpComponent, filterValueRenderers,
  expandedSectionsRenderer, bulkSelection, additionalAttributes,
  entityAttributesAreCamelCase, topRightCol,
}: Props<T>) => {
  const [urlQueryFilters, setUrlQueryFilters] = useUrlQueryFilters();
  const [query, setQuery] = useQueryParam('query', StringParam);
  const { layoutConfig, isInitialLoading: isLoadingLayoutPreferences } = useTableLayout(tableLayout);
  const paginationQueryParameter = usePaginationQueryParameter(undefined, layoutConfig.pageSize, false);
  const { mutate: updateTableLayout } = useUpdateUserLayoutPreferences(tableLayout.entityTableId);
  const fetchOptions = useMemo(() => ({
    query,
    page: paginationQueryParameter.page,
    pageSize: layoutConfig.pageSize,
    sort: layoutConfig.sort,
    filters: urlQueryFilters,
  }), [layoutConfig.pageSize, layoutConfig.sort, paginationQueryParameter.page, query, urlQueryFilters]);
  const fetchKey = useMemo(() => keyFn(fetchOptions), [fetchOptions, keyFn]);

  const {
    data: paginatedEntities = INITIAL_DATA,
    isInitialLoading: isLoadingEntities,
    refetch,
  } = useFetchEntities<T>({ fetchKey, searchParams: fetchOptions, enabled: !isLoadingLayoutPreferences, fetchData, humanName });

  const onChangeFilters = useCallback((newUrlQueryFilters: UrlQueryFilters) => {
    paginationQueryParameter.resetPage();
    setUrlQueryFilters(newUrlQueryFilters);
  }, [paginationQueryParameter, setUrlQueryFilters]);

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
    () => ([...(paginatedEntities?.attributes ?? []), ...additionalAttributes]),
    [additionalAttributes, paginatedEntities?.attributes],
  );

  if (isLoadingLayoutPreferences || isLoadingEntities) {
    return <Spinner />;
  }

  const { list, pagination: { total }, attributes } = paginatedEntities;

  return (
    <TableFetchContextProvider refetch={refetch} searchParams={fetchOptions} attributes={attributes}>
      <PaginatedList pageSize={layoutConfig.pageSize}
                     showPageSizeSelect={false}
                     totalItems={total}>
        <SearchRow>
          <SearchForm onSearch={onSearch}
                      onReset={onSearchReset}
                      query={query}
                      queryHelpComponent={queryHelpComponent}>
            <div style={{ marginBottom: 5 }}>
              <EntityFilters attributes={attributes}
                             urlQueryFilters={urlQueryFilters}
                             setUrlQueryFilters={onChangeFilters}
                             filterValueRenderers={filterValueRenderers} />
            </div>
          </SearchForm>
          {topRightCol}
        </SearchRow>
        <div>
          {list?.length === 0 ? (
            <NoSearchResult>No {humanName} have been found.</NoSearchResult>
          ) : (
            <EntityDataTable<T> data={list}
                                visibleColumns={layoutConfig.displayedAttributes}
                                columnsOrder={columnsOrder}
                                expandedSectionsRenderer={expandedSectionsRenderer}
                                bulkSelection={bulkSelection}
                                onColumnsChange={onColumnsChange}
                                onSortChange={onSortChange}
                                onPageSizeChange={onPageSizeChange}
                                pageSize={layoutConfig.pageSize}
                                activeSort={layoutConfig.sort}
                                rowActions={entityActions}
                                actionsCellWidth={actionsCellWidth}
                                columnRenderers={columnRenderers}
                                columnDefinitions={columnDefinitions}
                                entityAttributesAreCamelCase={entityAttributesAreCamelCase} />
          )}
        </div>
      </PaginatedList>
    </TableFetchContextProvider>
  );
};

PageEntityTable.defaultProps = {
  actionsCellWidth: 160,
  additionalAttributes: [],
  bulkSelection: undefined,
  entityAttributesAreCamelCase: undefined,
  expandedSectionsRenderer: undefined,
  filterValueRenderers: undefined,
  queryHelpComponent: undefined,
  topRightCol: undefined,
};

export default PageEntityTable;
