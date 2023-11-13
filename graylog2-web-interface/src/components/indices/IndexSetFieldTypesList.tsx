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

import type { IndexSetFieldType } from 'hooks/useIndexSetFieldType';
import { Button } from 'components/bootstrap';
import useIndexSetFieldTypes from 'hooks/useIndexSetFieldType';
import useParams from 'routing/useParams';
import {
  HoverForHelp,
  Icon,
  NoEntitiesExist,
  PaginatedList,
  Spinner,
} from 'components/common';
import EntityDataTable from 'components/common/EntityDataTable';
import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import type { Sort } from 'stores/PaginationTypes';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import ChangeFieldTypeModal from 'views/logic/fieldactions/ChangeFieldType/ChangeFieldTypeModal';
import useFiledTypes from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypes';

export const ENTITY_TABLE_ID = 'index-set-field-types';
export const DEFAULT_LAYOUT = {
  pageSize: 20,
  sort: { attributeId: 'field_name', direction: 'asc' } as Sort,
  displayedColumns: ['field_name', 'type', 'is_custom', 'is_reserved'],
  columnsOrder: ['field_name', 'type', 'is_custom', 'is_reserved'],
};

const IndexSetFieldTypesList = () => {
  const { indexSetId } = useParams();
  const [editingField, setEditingField] = useState<IndexSetFieldType | null>(null);

  const handleOnClose = useCallback(() => {
    setEditingField(null);
  }, []);

  const handleOnOpen = useCallback((fieldType: IndexSetFieldType) => {
    setEditingField(fieldType);
  }, []);

  const initialSelection = useMemo(() => [indexSetId], [indexSetId]);
  const [query] = useState('');
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

  const renderActions = useCallback((fieldType: IndexSetFieldType) => (
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
        {pagination?.total === 0 && !searchParams.query && (
          <NoEntitiesExist>
            No fields have been created yet.
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
    </>
  );
};

export default IndexSetFieldTypesList;
