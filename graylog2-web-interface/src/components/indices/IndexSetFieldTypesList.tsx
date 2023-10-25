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
import take from 'lodash/take';

import type { IndexSetFieldType } from 'hooks/useIndexSetFieldType';
import useIndexSetFieldTypes from 'hooks/useIndexSetFieldType';
import useParams from 'routing/useParams';
import { Button } from 'components/bootstrap';
import { NoEntitiesExist, NoSearchResult, PaginatedList, SearchForm, Spinner } from 'components/common';
import EntityDataTable from 'components/common/EntityDataTable';
import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import type { Sort } from 'stores/PaginationTypes';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import ChangeFieldTypeModal from 'views/logic/fieldactions/ChangeFieldType/ChangeFieldTypeModal';
import useFiledTypes from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypes';
import type { TypeHistoryItem } from 'views/logic/fieldactions/ChangeFieldType/types';

import QueryHelper from '../common/QueryHelper';

export const ENTITY_TABLE_ID = 'index-set-field-types';
export const DEFAULT_LAYOUT = {
  pageSize: 20,
  sort: { attributeId: 'field_name', direction: 'asc' } as Sort,
  displayedColumns: ['field_name', 'type', 'type_history'],
  columnsOrder: ['field_name', 'type', 'type_history'],
};

const IndexSetFieldTypesList = () => {
  const { indexSetId } = useParams();
  const [editingField, setEditingField] = useState(null);

  const handleOnClose = useCallback(() => {
    setEditingField(null);
  }, []);

  const handleOnOpen = useCallback((id: string) => {
    setEditingField(id);
  }, []);

  const initialSelection = useMemo(() => [indexSetId], [indexSetId]);
  const [query, setQuery] = useState('');
  const [activePage, setActivePage] = useState(1);
  const { data: { fieldTypes }, isLoading: isOptionsLoading } = useFiledTypes();
  const { layoutConfig, isInitialLoading: isLoadingLayoutPreferences } = useTableLayout({
    entityTableId: ENTITY_TABLE_ID,
    defaultPageSize: DEFAULT_LAYOUT.pageSize,
    defaultDisplayedAttributes: DEFAULT_LAYOUT.displayedColumns,
    defaultSort: DEFAULT_LAYOUT.sort,
  });
  const searchParams = useMemo(() => ({
    query,
    page: activePage,
    pageSize: layoutConfig.pageSize,
    sort: layoutConfig.sort,
  }), [activePage, layoutConfig.pageSize, layoutConfig.sort, query]);
  const { mutate: updateTableLayout } = useUpdateUserLayoutPreferences(ENTITY_TABLE_ID);
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
  const { isLoading, data: { list, pagination, attributes }, refetch } = useIndexSetFieldTypes(indexSetId, searchParams, { enabled: !isLoadingLayoutPreferences });

  const customColumnRenderers = useMemo(() => ({
    attributes: {
      type: {
        renderCell: (item: string) => <span>{fieldTypes[item]}</span>,
      },
      type_history: {
        renderCell: (items: Array<TypeHistoryItem>) => {
          const rest = take(items, items.length - 1).map((item) => fieldTypes[item] || item);
          if (!rest.length) return null;

          return (
            <span>
              {rest.join(', ')}
            </span>
          );
        },
      },
    },
  }), [fieldTypes]);

  const openEditModal = useCallback((fieldType: IndexSetFieldType) => {
    handleOnOpen(fieldType.fieldName);
  }, [handleOnOpen]);

  const renderActions = useCallback((fieldType: IndexSetFieldType) => (
    <Button onClick={() => openEditModal(fieldType)}
            role="button"
            bsSize="xsmall"
            bsStyle="danger"
            title={`Edit ${fieldType.fieldName} field type`}
            tabIndex={0}>
      Edit
    </Button>
  ), [openEditModal]);

  if (isLoadingLayoutPreferences || isLoading) {
    return <Spinner />;
  }

  return (
    <>
      <PaginatedList onChange={onPageChange}
                     totalItems={pagination?.total}
                     pageSize={layoutConfig.pageSize}
                     activePage={activePage}
                     showPageSizeSelect={false}
                     useQueryParameter={false}>
        <div style={{ marginBottom: '5px' }}>
          <SearchForm focusAfterMount
                      onSearch={onSearch}
                      queryHelpComponent={<QueryHelper entityName="search" commonFields={['id', 'title']} />}
                      topMargin={0}
                      onReset={onResetSearch} />
        </div>
        {pagination?.total === 0 && !searchParams.query && (
          <NoEntitiesExist>
            No fields have been created yet.
          </NoEntitiesExist>
        )}
        {pagination?.total === 0 && searchParams.query && (
          <NoSearchResult>
            No fields have been found.
          </NoSearchResult>
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
                                field={editingField}
                                onClose={handleOnClose}
                                show={editingField}
                                showSelectionTable={false}
                                fieldTypes={fieldTypes}
                                isOptionsLoading={isOptionsLoading}
                                onSubmitCallback={refetch} />
        ) : null
      }
    </>
  );
};

export default IndexSetFieldTypesList;
