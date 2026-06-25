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
import { useMemo, useEffect, useCallback } from 'react';
import styled, { css } from 'styled-components';

import type { LayoutConfig } from 'components/common/EntityDataTable/hooks/useTableLayout';
import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import { useTableEventHandlers } from 'components/common/EntityDataTable';
import {
  Spinner,
  PaginatedList,
  SearchForm,
  NoSearchResult,
  EntityDataTable,
  FetchErrorAlert,
} from 'components/common';
import type { Attribute, SearchParams } from 'stores/PaginationTypes';
import type {
  EntityBase,
  DefaultLayout,
  ExpandedSectionRenderers,
  RowOverride,
  SlicingPreferences,
  ColumnSchema,
} from 'components/common/EntityDataTable/types';
import EntityFilters from 'components/common/EntityFilters';
import type { UrlQueryFilters } from 'components/common/EntityFilters/types';
import TableFetchContextProvider from 'components/common/PaginatedEntityTable/TableFetchContextProvider';
import TableFilterProvider from 'components/common/PaginatedEntityTable/TableFilterProvider';
import type { PaginatedResponse, FetchOptions } from 'components/common/PaginatedEntityTable/useFetchEntities';
import useFetchEntities from 'components/common/PaginatedEntityTable/useFetchEntities';
import useOnRefresh from 'components/common/PaginatedEntityTable/useOnRefresh';
import Slicing, { type SliceRenderers } from 'components/common/PaginatedEntityTable/slicing';
import type { FetchSlices } from 'components/common/PaginatedEntityTable/slicing/useFetchSlices';
import useAuthorizedAttributes from 'components/common/PaginatedEntityTable/hooks/useAuthorizedAttributes';
import {
  ALPHABETICAL_SORT,
  defaultSortDirectionForMode,
} from 'components/common/PaginatedEntityTable/slicing/slicingConstants';

import { useWithLocalState, useWithURLParams } from './useFiltersAndPagination';
import useTableFilterContext from './useTableFilterContext';

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
    flex-direction: column;
    gap: ${theme.spacings.sm};
    width: 100%;
  `,
);

const RowContainer = styled.div(
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

const defaultSlicingPreferences = (
  sliceColumn: string | null,
  columnSchemas: Array<ColumnSchema>,
): SlicingPreferences => {
  if (sliceColumn === null)
    return {
      sliceColumn: null,
      sortBy: null,
      order: null,
    };

  const sliceSortDefault = columnSchemas.find(({ id }) => id === sliceColumn)?.slice_sort_default;
  const sortBy = sliceSortDefault?.mode ?? ALPHABETICAL_SORT;

  return {
    sliceColumn,
    sortBy,
    order: defaultSortDirectionForMode(sortBy, sliceSortDefault),
  };
};

type PaginatedEntityTableInnerProps<T extends EntityBase, M> = Omit<
  PaginatedEntityTableProps<T, M>,
  'defaultFilters' | 'fetchOptions'
> & {
  isLoadingLayoutPreferences: boolean;
  layoutConfig: LayoutConfig;
  onDataLoaded?: (data: PaginatedResponse<unknown, unknown>) => void;
  reactQueryOptions: FetchOptions;
};

const PaginatedEntityTableInner = <T extends EntityBase, M = unknown>({
  additionalAttributes = [],
  bulkSelection = undefined,
  columnRenderers,
  entityActions = undefined,
  entityAttributesAreCamelCase,
  expandedSectionRenderers = undefined,
  rowOverride = undefined,
  externalSearch = undefined,
  fetchEntities,
  fetchSlices = undefined,
  filterValueRenderers = undefined,
  focusSearchAfterMount = false,
  humanName,
  isLoadingLayoutPreferences,
  keyFn,
  layoutConfig,
  middleSection: MiddleSection = undefined,
  topSection: TopSection = undefined,
  onDataLoaded = undefined,
  queryHelpComponent = undefined,
  reactQueryOptions,
  searchPlaceholder = undefined,
  sliceRenderers = undefined,
  tableLayout,
  topRightCol = undefined,
  withoutURLParams = false,
  noPageSizeSelect = false,
  noColumnReordering = false,
}: PaginatedEntityTableInnerProps<T, M>) => {
  const { searchParams, setQuery, onChangeFilters, onChangeSlicingFilter, paginationState } = useTableFilterContext();
  const { mutateAsync: updateTableLayout, resetAsync: resetTableLayout } = useUpdateUserLayoutPreferences(
    tableLayout.entityTableId,
    tableLayout.layoutVariant,
  );
  const fetchKey = useMemo(() => keyFn(searchParams), [searchParams, keyFn]);

  const {
    data: paginatedEntities = INITIAL_DATA,
    isInitialLoading: isLoadingEntities,
    refetch,
    isError,
    error,
  } = useFetchEntities<T, M>({
    fetchKey,
    searchParams,
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
    resetTableLayout,
  });

  const columnSchemas = useMemo(
    () => [
      ...(paginatedEntities?.attributes ?? []),
      ...additionalAttributes.map((attribute) => ({ isDerived: true, ...attribute })),
    ],
    [additionalAttributes, paginatedEntities?.attributes],
  );

  const onSlicingPreferencesChange = useCallback(
    (slicing: SlicingPreferences) => updateTableLayout({ slicing }),
    [updateTableLayout],
  );

  const onSlicingLayout = useCallback(
    (newSliceCol: string | undefined, newSlice?: string) => {
      onChangeSlicingFilter(newSlice);

      if (newSliceCol === searchParams.sliceCol) {
        return;
      }

      updateTableLayout({
        slicing: newSliceCol !== undefined ? defaultSlicingPreferences(newSliceCol, columnSchemas) : null,
      });
    },
    [columnSchemas, searchParams.sliceCol, onChangeSlicingFilter, updateTableLayout],
  );

  const attributes = useAuthorizedAttributes(paginatedEntities?.attributes ?? []);

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
  } = paginatedEntities;

  return (
    <TableFetchContextProvider refetch={refetch} attributes={attributes} entityTableId={tableLayout.entityTableId}>
      <Container>
        {TopSection ? <TopSection /> : null}
        <RowContainer>
          {searchParams.sliceCol && typeof fetchSlices === 'function' && (
            <Slicing
              fetchSlices={fetchSlices}
              appSection={appSection}
              columnSchemas={columnSchemas}
              sliceRenderers={sliceRenderers}
              onChangeSlicing={onSlicingLayout}
              onSlicingPreferencesChange={onSlicingPreferencesChange}
              slicingPreferences={layoutConfig.slicing}
            />
          )}
          <TableWrapper>
            {!externalSearch && (
              <SearchRow>
                <SearchForm
                  focusAfterMount={focusSearchAfterMount}
                  onSearch={onSearch}
                  onReset={onSearchReset}
                  query={searchParams.query}
                  placeholder={searchPlaceholder ?? `Search for ${humanName}`}
                  queryHelpComponent={queryHelpComponent}>
                  {attributes.length > 0 && (
                    <div style={{ marginBottom: 5 }}>
                      <EntityFilters
                        attributes={attributes}
                        urlQueryFilters={searchParams.filters}
                        setUrlQueryFilters={onChangeFilters}
                        filterValueRenderers={filterValueRenderers}
                        appSection={appSection}
                        activeSliceCol={searchParams.sliceCol}
                        activeSlice={searchParams.slice}
                      />
                    </div>
                  )}
                </SearchForm>
                {topRightCol}
              </SearchRow>
            )}
            {MiddleSection ? <MiddleSection searchParams={searchParams} setFilters={onChangeFilters} /> : null}

            {isError ? (
              <FetchErrorAlert message={`Fetching ${humanName} failed`} error={error} />
            ) : (
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
                    onChangeSlicing={onSlicingLayout}
                    expandedSectionRenderers={expandedSectionRenderers}
                    rowOverride={rowOverride}
                    enableSlicing={typeof fetchSlices === 'function' && !layoutConfig?.slicing?.readOnly}
                    bulkSelection={bulkSelection}
                    onSortChange={onSortChange}
                    onPageSizeChange={onPageSizeChange}
                    pageSize={layoutConfig.pageSize}
                    activeSort={layoutConfig.sort}
                    activeSliceCol={searchParams.sliceCol}
                    appSection={appSection}
                    entityActions={entityActions}
                    columnRenderers={columnRenderers}
                    columnSchemas={columnSchemas}
                    entityAttributesAreCamelCase={entityAttributesAreCamelCase}
                    meta={meta}
                    noPageSizeSelect={noPageSizeSelect}
                    noColumnReordering={noColumnReordering}
                  />
                )}
              </PaginatedList>
            )}
          </TableWrapper>
        </RowContainer>
      </Container>
    </TableFetchContextProvider>
  );
};

type WrapperProps<T extends EntityBase, M> = PaginatedEntityTableProps<T, M> & {
  isLoadingLayoutPreferences: boolean;
  layoutConfig: LayoutConfig;
  reactQueryOptions: FetchOptions;
  onDataLoaded?: (data: PaginatedResponse<T, M>) => void;
};

const TableWithLocalState = <T extends EntityBase, M = unknown>({ ...props }: WrapperProps<T, M>) => {
  const contextValue = useWithLocalState(props.layoutConfig, props.defaultFilters);

  return (
    <TableFilterProvider value={contextValue} externalSearch={props.externalSearch}>
      <PaginatedEntityTableInner<T, M> {...props} />
    </TableFilterProvider>
  );
};

const TableWithURLParams = <T extends EntityBase, M = unknown>({ ...props }: WrapperProps<T, M>) => {
  const contextValue = useWithURLParams(props.layoutConfig, props.defaultFilters);

  return (
    <TableFilterProvider value={contextValue} externalSearch={props.externalSearch}>
      <PaginatedEntityTableInner<T, M> {...props} />
    </TableFilterProvider>
  );
};

export type PaginatedEntityTableProps<T extends EntityBase, M> = {
  additionalAttributes?: Array<Attribute>;
  bulkSelection?: EntityDataTableProps['bulkSelection'];
  columnRenderers: EntityDataTableProps['columnRenderers'];
  defaultFilters?: UrlQueryFilters;
  entityActions?: EntityDataTableProps['entityActions'];
  entityAttributesAreCamelCase: boolean;
  expandedSectionRenderers?: ExpandedSectionRenderers<T>;
  rowOverride?: RowOverride<T>;
  externalSearch?: ExternalSearch;
  fetchEntities: (options: SearchParams) => Promise<PaginatedResponse<T, M>>;
  fetchOptions?: FetchOptions;
  fetchSlices?: FetchSlices;
  filterValueRenderers?: React.ComponentProps<typeof EntityFilters>['filterValueRenderers'];
  focusSearchAfterMount?: boolean;
  humanName: string;
  keyFn: (options: SearchParams) => Array<unknown>;
  middleSection?: React.ComponentType<MiddleSectionProps>;
  topSection?: React.ComponentType;
  onDataLoaded?: (data: PaginatedResponse<T, M>) => void;
  queryHelpComponent?: React.ReactNode;
  searchPlaceholder?: string;
  sliceRenderers?: SliceRenderers;
  tableLayout: DefaultLayout;
  topRightCol?: React.ReactNode;
  withoutURLParams?: boolean;
  noPageSizeSelect?: boolean;
  noColumnReordering?: boolean;
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
