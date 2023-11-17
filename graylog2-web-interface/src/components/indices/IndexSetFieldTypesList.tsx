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
import React, { useCallback, useMemo, useState } from 'react';
import styled, { css } from 'styled-components';
import { useQueryParam, StringParam } from 'use-query-params';

import type { IndexSetFieldType } from 'hooks/useIndexSetFieldType';
import { Button } from 'components/bootstrap';
import useIndexSetFieldTypes from 'hooks/useIndexSetFieldType';
import useParams from 'routing/useParams';
import {
  HoverForHelp,
  Icon,
  NoEntitiesExist,
  PaginatedList, SearchForm,
  Spinner,
} from 'components/common';
import EntityDataTable from 'components/common/EntityDataTable';
import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import type { Sort } from 'stores/PaginationTypes';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import ChangeFieldTypeModal from 'views/logic/fieldactions/ChangeFieldType/ChangeFieldTypeModal';
import IndexSetCustomFieldTypeRemoveModal from 'components/indices/IndexSetCustomFieldTypeRemoveModal';
import useFiledTypes from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypes';
import EntityFilters from 'components/common/EntityFilters';
import useUrlQueryFilters from 'components/common/EntityFilters/hooks/useUrlQueryFilters';
import type { UrlQueryFilters } from 'components/common/EntityFilters/types';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';

export const ENTITY_TABLE_ID = 'index-set-field-types';
export const DEFAULT_LAYOUT = {
  pageSize: 20,
  sort: { attributeId: 'field_name', direction: 'asc' } as Sort,
  displayedColumns: ['field_name', 'type', 'is_custom', 'is_reserved'],
  columnsOrder: ['field_name', 'type', 'is_custom', 'is_reserved'],
};

const StyledIcon = styled(Icon)<{ $value: 'true' | 'false' }>(({ theme, $value }) => css`
  color: ${$value === 'true' ? theme.colors.variant.success : theme.colors.variant.danger};
  margin-right: 5px;
`);

const FilterValueRenderers = {
  is_custom: (value: 'true' | 'false', title: string) => (
    <>
      <StyledIcon name={value === 'true' ? 'circle-check' : 'circle-xmark'} $value={value} />
      {title}
    </>
  ),
  is_reserved: (value: 'true' | 'false', title: string) => (
    <>
      <StyledIcon name={value === 'true' ? 'circle-check' : 'circle-xmark'} $value={value} />
      {title}
    </>
  ),
};

const IndexSetFieldTypesList = () => {
  const { indexSetId } = useParams();
  const [editingField, setEditingField] = useState<IndexSetFieldType | null>(null);
  const [deletingFieldTypes, setDeletingFieldTypes] = useState<Array<string> | null>(null);

  const handleOnClose = useCallback(() => {
    setEditingField(null);
  }, []);

  const handleOnOpen = useCallback((fieldType: IndexSetFieldType) => {
    setEditingField(fieldType);
  }, []);

  const initialSelection = useMemo(() => [indexSetId], [indexSetId]);
  const [urlQueryFilters, setUrlQueryFilters] = useUrlQueryFilters();
  const [query, setQuery] = useQueryParam('query', StringParam);
  const { data: { fieldTypes }, isLoading: isOptionsLoading } = useFiledTypes();
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
  const { isLoading, data: { list, pagination, attributes }, refetch } = useIndexSetFieldTypes(indexSetId, searchParams, { enabled: !isLoadingLayoutPreferences });

  const customColumnRenderers = useMemo(() => ({
    attributes: {
      type: {
        renderCell: (item: string) => <span>{fieldTypes[item]}</span>,
      },
      is_custom: {
        renderCell: (isCustom: boolean) => (isCustom ? <Icon title="Field has custom field type" name="check" /> : null),
        staticWidth: 120,
      },
      is_reserved: {
        renderCell: (isReserved: boolean) => (isReserved ? <Icon title="Field has reserved field type" name="check" /> : null),
        staticWidth: 120,
      },
    },
  }), [fieldTypes]);

  const openEditModal = useCallback((fieldType: IndexSetFieldType) => {
    handleOnOpen(fieldType);
  }, [handleOnOpen]);

  const openDeletingModal = useCallback((fields: Array<string>) => {
    setDeletingFieldTypes(fields);
  }, []);
  const onCloseDeleting = useCallback(() => setDeletingFieldTypes(null), []);

  const renderActions = useCallback((fieldType: IndexSetFieldType) => (
    <>
      <Button onClick={() => openEditModal(fieldType)}
              role="button"
              bsSize="xsmall"
              disabled={fieldType.isReserved}
              title={`Edit field type for ${fieldType.fieldName}`}
              tabIndex={0}>
        Edit {
            fieldType.isReserved && (
            <HoverForHelp displayLeftMargin title="Reserved field is not editable" pullRight={false}>
              We use reserved fields internally and expect a certain structure from them. Changing the field type for reserved fields might impact the stability of Graylog
            </HoverForHelp>
            )
        }
      </Button>
      {fieldType.isCustom && (
        <Button onClick={() => openDeletingModal([fieldType.fieldName])}
                role="button"
                bsSize="xsmall"
                bsStyle="danger"
                title="Remove custom type"
                tabIndex={0}>
          Remove
        </Button>
      )}
    </>
  ), [openDeletingModal, openEditModal]);
  const indexSetsDeleting = useMemo(() => [indexSetId], [indexSetId]);

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
    <>
      <PaginatedList totalItems={pagination?.total}
                     pageSize={layoutConfig.pageSize}
                     showPageSizeSelect={false}>
        <div style={{ marginBottom: 5 }}>
          <SearchForm onSearch={onSearch}
                      onReset={onSearchReset}
                      query={query}
                      placeholder="Enter search query for the filed name...">
            <EntityFilters attributes={attributes}
                           urlQueryFilters={urlQueryFilters}
                           setUrlQueryFilters={onChangeFilters}
                           filterValueRenderers={FilterValueRenderers} />
          </SearchForm>
        </div>
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
                                              rowActions={renderActions} />
        )}
      </PaginatedList>
      {
        editingField ? (
          <ChangeFieldTypeModal initialSelectedIndexSets={initialSelection}
                                field={editingField.fieldName}
                                onClose={handleOnClose}
                                show={!!editingField}
                                showSelectionTable={false}
                                fieldTypes={fieldTypes}
                                isOptionsLoading={isOptionsLoading}
                                onSubmitCallback={refetch}
                                initialFieldType={editingField.type} />
        ) : null
      }
      {
        deletingFieldTypes ? (
          <IndexSetCustomFieldTypeRemoveModal show={!!deletingFieldTypes} fields={deletingFieldTypes} onClose={onCloseDeleting} indexSetIds={indexSetsDeleting} />
        ) : null
      }
    </>
  );
};

export default IndexSetFieldTypesList;
