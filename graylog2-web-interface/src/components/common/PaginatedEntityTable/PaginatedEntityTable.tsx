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
import { useMemo, useEffect } from 'react';
import styled, { css } from 'styled-components';

import type { LayoutConfig } from 'components/common/EntityDataTable/hooks/useTableLayout';
import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import { useTableEventHandlers } from 'components/common/EntityDataTable';
import { Spinner, PaginatedList, SearchForm, NoSearchResult, EntityDataTable } from 'components/common';
import type { Attribute, SearchParams } from 'stores/PaginationTypes';
import type { EntityBase, DefaultLayout, ExpandedSectionRenderers } from 'components/common/EntityDataTable/types';
import EntityFilters from 'components/common/EntityFilters';
import type { UrlQueryFilters } from 'components/common/EntityFilters/types';
import TableFetchContextProvider from 'components/common/PaginatedEntityTable/TableFetchContextProvider';
import type { PaginatedResponse, FetchOptions } from 'components/common/PaginatedEntityTable/useFetchEntities';
import useFetchEntities from 'components/common/PaginatedEntityTable/useFetchEntities';
import useOnRefresh from 'components/common/PaginatedEntityTable/useOnRefresh';
import type { PaginationQueryParameterResult } from 'hooks/usePaginationQueryParameter';
import Slicing, { type SliceRenderers } from 'components/common/PaginatedEntityTable/slicing';
import type { FetchSlices } from 'components/common/PaginatedEntityTable/slicing/useFetchSlices';

import { useWithLocalState, useWithURLParams } from './useFiltersAndPagination';

const SearchRow = styled.div`
  margin-bottom: 5px;
  display: flex;
  width: 100%;
  justify-content: space-between;
  align-items: center;
`;

const Container = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: row;
    gap: ${theme.spacings.sm};
    width: 100%;
  `,
);

const TableWrapper = styled.div`
  display: flex;
  flex-direction: column;
  overflow-x: auto;
  width: 100%;
`;

type EntityDataTableProps = React.ComponentProps<typeof EntityDataTable>;
type ExternalSearch = {
  query: string;
  onSearch?: (query: string) => void;
  onReset?: () => void;
};
export type MiddleSectionProps = {
  searchParams: SearchParams;
  setFilters: (newFilters: UrlQueryFilters) => void;
};

const INITIAL_DATA = {
  pagination: { total: 0 },
  list: [],
  attributes: [],
  meta: null,
};

type InnerProps = {
  fetchOptions: SearchParams;
  isLoadingLayoutPreferences: boolean;
  layoutConfig: LayoutConfig;
  onDataLoaded?: (data: PaginatedResponse<unknown, unknown>) => void;
  onChangeFilters: (newFilters: UrlQueryFilters) => void;
  onChangeSlicing: (sliceCol: string | undefined, slice?: string) => void;
  paginationState: PaginationQueryParameterResult;
  reactQueryOptions: FetchOptions;
  setQuery: (newQuery: string) => void;
};

const PaginatedEntityTableInner = <T extends EntityBase, M = unknown>({
  additionalAttributes = [],
  bulkSelection = undefined,
  columnRenderers,
  entityActions = undefined,
  entityAttributesAreCamelCase,
  expandedSectionRenderers = undefined,
  externalSearch = undefined,
  fetchEntities,
  fetchOptions,
  fetchSlices = undefined,
  filterValueRenderers = undefined,
  focusSearchAfterMount = false,
  humanName,
  isLoadingLayoutPreferences,
  keyFn,
  layoutConfig,
  middleSection: MiddleSection = undefined,
  onChangeFilters,
  onChangeSlicing,
  onDataLoaded = undefined,
  paginationState,
  queryHelpComponent = undefined,
  reactQueryOptions,
  searchPlaceholder = undefined,
  sliceRenderers = undefined,
  setQuery,
  tableLayout,
  topRightCol = undefined,
  withoutURLParams = false,
}: PaginatedEntityTableProps<T, M> & InnerProps) => {
  const { mutateAsync: updateTableLayout } = useUpdateUserLayoutPreferences(tableLayout.entityTableId);
  const fetchKey = useMemo(() => keyFn(fetchOptions), [fetchOptions, keyFn]);

  const {
    data: paginatedEntities = INITIAL_DATA,
    isInitialLoading: isLoadingEntities,
    refetch,
  } = useFetchEntities<T, M>({
    fetchKey,
    searchParams: fetchOptions,
    enabled: !isLoadingLayoutPreferences,
    fetchEntities,
    humanName,
    fetchOptions: reactQueryOptions,
  });

  useOnRefresh(refetch);

  useEffect(() => {
    if (!onDataLoaded || isLoadingEntities) {
      return;
    }

    onDataLoaded(paginatedEntities);
  }, [isLoadingEntities, onDataLoaded, paginatedEntities]);

  const appSection = `${tableLayout.entityTableId}-list`;

  const {
    onLayoutPreferencesChange,
    onPageSizeChange,
    onResetLayoutPreferences,
    onSearch,
    onSearchReset,
    onSortChange,
  } = useTableEventHandlers({
    appSection,
    paginationQueryParameter: paginationState,
    updateTableLayout,
    setQuery,
  });

  const columnSchemas = useMemo(
    () => [
      ...(paginatedEntities?.attributes ?? []),
      ...additionalAttributes.map((attribute) => ({ isDerived: true, ...attribute })),
    ],
    [additionalAttributes, paginatedEntities?.attributes],
  );

  const onPaginationChange = withoutURLParams
    ? (currentPage: number, pageSize: number) => paginationState.setPagination({ page: currentPage, pageSize })
    : undefined;

  if (isLoadingLayoutPreferences || isLoadingEntities) {
    return <Spinner />;
  }

  const {
    list,
    meta,
    pagination: { total },
    attributes,
  } = paginatedEntities;

  return (
    <TableFetchContextProvider
      refetch={refetch}
      searchParams={fetchOptions}
      attributes={attributes}
      entityTableId={tableLayout.entityTableId}>
      <Container>
        {fetchOptions.sliceCol && (
          <Slicing
            fetchSlices={fetchSlices}
            appSection={appSection}
            columnSchemas={columnSchemas}
            sliceRenderers={sliceRenderers}
            onChangeSlicing={onChangeSlicing}
          />
        )}
        <TableWrapper>
          {!externalSearch && (
            <SearchRow>
              <SearchForm
                focusAfterMount={focusSearchAfterMount}
                onSearch={onSearch}
                onReset={onSearchReset}
                query={fetchOptions.query}
                placeholder={searchPlaceholder ?? `Search for ${humanName}`}
                queryHelpComponent={queryHelpComponent}>
                <div style={{ marginBottom: 5 }}>
                  <EntityFilters
                    attributes={attributes}
                    urlQueryFilters={fetchOptions.filters}
                    setUrlQueryFilters={onChangeFilters}
                    filterValueRenderers={filterValueRenderers}
                    appSection={appSection}
                  />
                </div>
              </SearchForm>
              {topRightCol}
            </SearchRow>
          )}
          {MiddleSection ? <MiddleSection searchParams={fetchOptions} setFilters={onChangeFilters} /> : null}

          <PaginatedList
            pageSize={layoutConfig.pageSize}
            showPageSizeSelect={false}
            totalItems={total}
            useQueryParameter={!withoutURLParams}
            onChange={onPaginationChange}>
            {list?.length === 0 ? (
              <NoSearchResult>No {humanName} have been found.</NoSearchResult>
            ) : (
              <EntityDataTable<T, M>
                entities={list}
                defaultDisplayedColumns={tableLayout.defaultDisplayedAttributes}
                layoutPreferences={{
                  attributes: layoutConfig.attributes,
                  order: layoutConfig.order,
                }}
                defaultColumnOrder={tableLayout.defaultColumnOrder}
                onResetLayoutPreferences={onResetLayoutPreferences}
                onLayoutPreferencesChange={onLayoutPreferencesChange}
                onChangeSlicing={onChangeSlicing}
                expandedSectionRenderers={expandedSectionRenderers}
                bulkSelection={bulkSelection}
                onSortChange={onSortChange}
                onPageSizeChange={onPageSizeChange}
                pageSize={layoutConfig.pageSize}
                activeSort={layoutConfig.sort}
                activeSliceCol={fetchOptions.sliceCol}
                appSection={appSection}
                entityActions={entityActions}
                columnRenderers={columnRenderers}
                columnSchemas={columnSchemas}
                entityAttributesAreCamelCase={entityAttributesAreCamelCase}
                meta={meta}
              />
            )}
          </PaginatedList>
        </TableWrapper>
      </Container>
    </TableFetchContextProvider>
  );
};

type WrapperProps<T, M> = PaginatedEntityTableProps<T, M> & {
  isLoadingLayoutPreferences: boolean;
  layoutConfig: LayoutConfig;
  reactQueryOptions: FetchOptions;
  onDataLoaded?: (data: PaginatedResponse<T, M>) => void;
};

const TableWithLocalState = <T extends EntityBase, M = unknown>({ ...props }: WrapperProps<T, M>) => {
  const { fetchOptions, setQuery, onChangeFilters, onChangeSlicing, paginationState } = useWithLocalState(
    props.layoutConfig,
  );
  const effectiveFetchOptions = props.externalSearch
    ? { ...fetchOptions, query: props.externalSearch.query }
    : fetchOptions;

  return (
    <PaginatedEntityTableInner<T, M>
      {...props}
      fetchOptions={effectiveFetchOptions}
      setQuery={props.externalSearch ? () => {} : setQuery}
      onChangeSlicing={onChangeSlicing}
      onChangeFilters={onChangeFilters}
      paginationState={paginationState}
      onDataLoaded={props.onDataLoaded}
    />
  );
};

const TableWithURLParams = <T extends EntityBase, M = unknown>({ ...props }: WrapperProps<T, M>) => {
  const { fetchOptions, setQuery, onChangeFilters, paginationState, onChangeSlicing } = useWithURLParams(
    props.layoutConfig,
  );
  const effectiveFetchOptions = props.externalSearch
    ? { ...fetchOptions, query: props.externalSearch.query }
    : fetchOptions;

  return (
    <PaginatedEntityTableInner<T, M>
      {...props}
      fetchOptions={effectiveFetchOptions}
      setQuery={props.externalSearch ? () => {} : setQuery}
      onChangeFilters={onChangeFilters}
      onChangeSlicing={onChangeSlicing}
      paginationState={paginationState}
      onDataLoaded={props.onDataLoaded}
    />
  );
};

export type PaginatedEntityTableProps<T, M> = {
  additionalAttributes?: Array<Attribute>;
  bulkSelection?: EntityDataTableProps['bulkSelection'];
  columnRenderers: EntityDataTableProps['columnRenderers'];
  entityActions?: EntityDataTableProps['entityActions'];
  entityAttributesAreCamelCase: boolean;
  expandedSectionRenderers?: ExpandedSectionRenderers<T>;
  externalSearch?: ExternalSearch;
  fetchEntities: (options: SearchParams) => Promise<PaginatedResponse<T, M>>;
  fetchOptions?: FetchOptions;
  fetchSlices?: FetchSlices;
  filterValueRenderers?: React.ComponentProps<typeof EntityFilters>['filterValueRenderers'];
  focusSearchAfterMount?: boolean;
  humanName: string;
  keyFn: (options: SearchParams) => Array<unknown>;
  middleSection?: React.ComponentType<MiddleSectionProps>;
  onDataLoaded?: (data: PaginatedResponse<T, M>) => void;
  queryHelpComponent?: React.ReactNode;
  searchPlaceholder?: string;
  sliceRenderers?: SliceRenderers;
  tableLayout: DefaultLayout;
  topRightCol?: React.ReactNode;
  withoutURLParams?: boolean;
};

/*
 * This component is a wrapper for the EntityDataTable.
 * It contains all the required logic to either sync the URL query params or, alternatively, use local state for filters and pagination.
 * It should not be used when there are multiple entity tables on the page or when the table is rendered in a modal.
 */
const PaginatedEntityTable = <T extends EntityBase, M = unknown>({
  fetchOptions: reactQueryOptions = undefined,
  ...props
}: PaginatedEntityTableProps<T, M>) => {
  const { layoutConfig, isInitialLoading: isLoadingLayoutPreferences } = useTableLayout(props.tableLayout);

  const Wrapper = props.withoutURLParams ? TableWithLocalState : TableWithURLParams;

  return (
    <Wrapper<T, M>
      {...props}
      layoutConfig={layoutConfig}
      isLoadingLayoutPreferences={isLoadingLayoutPreferences}
      reactQueryOptions={reactQueryOptions}
      onDataLoaded={props.onDataLoaded}
    />
  );
};

export default PaginatedEntityTable;
