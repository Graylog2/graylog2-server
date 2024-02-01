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
import React, { useCallback, useMemo } from 'react';
import styled, { css } from 'styled-components';
import { useQueryParam, StringParam } from 'use-query-params';

import useIndexSetFieldTypes from 'components/indices/IndexSetFieldTypes/hooks/useIndexSetFieldType';
import useParams from 'routing/useParams';
import {
  Icon,
  NoEntitiesExist,
  PaginatedList, SearchForm,
  Spinner,
} from 'components/common';
import EntityDataTable from 'components/common/EntityDataTable';
import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import type { Sort } from 'stores/PaginationTypes';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import EntityFilters from 'components/common/EntityFilters';
import useUrlQueryFilters from 'components/common/EntityFilters/hooks/useUrlQueryFilters';
import type { UrlQueryFilters } from 'components/common/EntityFilters/types';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import FieldTypeActions from 'components/indices/IndexSetFieldTypes/FieldTypeActions';
import expandedSections from 'components/indices/IndexSetFieldTypes/originExpandedSections/expandedSections';
import hasOverride from 'components/indices/helpers/hasOverride';
import type { FieldTypeOrigin, IndexSetFieldType } from 'components/indices/IndexSetFieldTypes/types';
import OriginFilterValueRenderer from 'components/indices/IndexSetFieldTypes/OriginFilterValueRenderer';
import useCustomColumnRenderers from 'components/indices/IndexSetFieldTypes/hooks/useCustomColumnRenderers';
import IndexSetProfile from 'components/indices/IndexSetFieldTypes/IndexSetProfile';

import BulkActions from './BulkActions';

export const ENTITY_TABLE_ID = 'index-set-field-types';
export const DEFAULT_LAYOUT = {
  pageSize: 20,
  sort: { attributeId: 'field_name', direction: 'asc' } as Sort,
  displayedColumns: ['field_name', 'type', 'origin', 'is_reserved'],
  columnsOrder: ['field_name', 'type', 'origin', 'is_reserved'],
};

const StyledIcon = styled(Icon)<{ $value: 'true' | 'false' }>(({ theme, $value }) => css`
  color: ${$value === 'true' ? theme.colors.variant.success : theme.colors.variant.danger};
  margin-right: 5px;
`);

const StyledTopRow = styled.div`
  margin-bottom: 5px;
  display: flex;
  width: 100%;
  justify-content: space-between;
  align-items: center;
`;
const isEntitySelectable = (fieldType: IndexSetFieldType) => hasOverride(fieldType);
const FilterValueRenderers = {
  is_reserved: (value: 'true' | 'false', title: string) => (
    <>
      <StyledIcon name={value === 'true' ? 'circle-check' : 'circle-xmark'} $value={value} />
      {title}
    </>
  ),
  origin: (value: FieldTypeOrigin, title: string) => <OriginFilterValueRenderer title={title} origin={value} />,
};

const IndexSetFieldTypesList = () => {
  const { indexSetId } = useParams();
  const [urlQueryFilters, setUrlQueryFilters] = useUrlQueryFilters();
  const [query, setQuery] = useQueryParam('query', StringParam);

  const { layoutConfig, isInitialLoading: isLoadingLayoutPreferences } = useTableLayout({
    entityTableId: ENTITY_TABLE_ID,
    defaultPageSize: DEFAULT_LAYOUT.pageSize,
    defaultDisplayedAttributes: DEFAULT_LAYOUT.displayedColumns,
    defaultSort: DEFAULT_LAYOUT.sort,
  });
  const paginationQueryParameter = usePaginationQueryParameter(undefined, layoutConfig.pageSize, false);
  const searchParams = useMemo(() => ({
    query,
    page: paginationQueryParameter.page,
    pageSize: layoutConfig.pageSize,
    sort: layoutConfig.sort,
    filters: urlQueryFilters,
  }), [paginationQueryParameter.page, layoutConfig.pageSize, layoutConfig.sort, query, urlQueryFilters]);
  const { mutate: updateTableLayout } = useUpdateUserLayoutPreferences(ENTITY_TABLE_ID);

  const onPageSizeChange = useCallback((newPageSize: number) => {
    paginationQueryParameter.resetPage();
    updateTableLayout({ perPage: newPageSize });
  }, [paginationQueryParameter, updateTableLayout]);

  const onSortChange = useCallback((newSort: Sort) => {
    paginationQueryParameter.resetPage();
    updateTableLayout({ sort: newSort });
  }, [paginationQueryParameter, updateTableLayout]);

  const onColumnsChange = useCallback((displayedAttributes: Array<string>) => {
    updateTableLayout({ displayedAttributes });
  }, [updateTableLayout]);
  const {
    isLoading,
    data: { list, pagination, attributes },
    refetch: refetchFieldTypes,
  } = useIndexSetFieldTypes(
    indexSetId,
    searchParams,
    { enabled: !isLoadingLayoutPreferences },
  );

  const customColumnRenderers = useCustomColumnRenderers(attributes);

  const renderActions = useCallback((fieldType: IndexSetFieldType) => (
    <FieldTypeActions fieldType={fieldType}
                      indexSetId={indexSetId}
                      refetchFieldTypes={refetchFieldTypes} />
  ), [indexSetId, refetchFieldTypes]);

  const onSearch = useCallback((val: string) => {
    paginationQueryParameter.resetPage();
    setQuery(val);
  }, [paginationQueryParameter, setQuery]);
  const onSearchReset = useCallback(() => setQuery(''), [setQuery]);
  const onChangeFilters = useCallback((newUrlQueryFilters: UrlQueryFilters) => {
    paginationQueryParameter.resetPage();
    setUrlQueryFilters(newUrlQueryFilters);
  }, [paginationQueryParameter, setUrlQueryFilters]);

  if (isLoadingLayoutPreferences || isLoading) {
    return <Spinner />;
  }

  return (
    <PaginatedList totalItems={pagination?.total}
                   pageSize={layoutConfig.pageSize}
                   showPageSizeSelect={false}>
      <StyledTopRow>
        <SearchForm onSearch={onSearch}
                    onReset={onSearchReset}
                    query={query}
                    placeholder="Enter search query for the field name...">
          <EntityFilters attributes={attributes}
                         urlQueryFilters={urlQueryFilters}
                         setUrlQueryFilters={onChangeFilters}
                         filterValueRenderers={FilterValueRenderers} />
        </SearchForm>
        <IndexSetProfile />
      </StyledTopRow>
      {pagination?.total === 0 && (
        <NoEntitiesExist>
          No fields have been found.
        </NoEntitiesExist>
      )}
      {!!list?.length && (
        <EntityDataTable<IndexSetFieldType> data={list}
                                            visibleColumns={layoutConfig.displayedAttributes}
                                            columnsOrder={DEFAULT_LAYOUT.columnsOrder}
                                            onColumnsChange={onColumnsChange}
                                            onSortChange={onSortChange}
                                            activeSort={layoutConfig.sort}
                                            pageSize={searchParams.pageSize}
                                            onPageSizeChange={onPageSizeChange}
                                            actionsCellWidth={120}
                                            columnRenderers={customColumnRenderers}
                                            columnDefinitions={attributes}
                                            rowActions={renderActions}
                                            expandedSectionsRenderer={expandedSections}
                                            bulkSelection={{
                                              actions: <BulkActions indexSetId={indexSetId} />,
                                              isEntitySelectable,
                                            }} />
      )}
    </PaginatedList>
  );
};

export default IndexSetFieldTypesList;
