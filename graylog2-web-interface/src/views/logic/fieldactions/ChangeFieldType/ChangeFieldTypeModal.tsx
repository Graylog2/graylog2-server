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
import React, { useMemo, useCallback, useState } from 'react';
import styled from 'styled-components';

import {
  PaginatedList, SearchForm, Spinner, NoSearchResult, NoEntitiesExist,
  EntityDataTable,
  Icon,
  NoEntitiesExist, NoSearchResult,
  SearchForm, Select,
} from 'components/common';
import { Button, BootstrapModalForm, Alert, Input } from 'components/bootstrap';
import { DEFAULT_LAYOUT, ENTITY_TABLE_ID } from 'views/logic/fieldactions/ChangeFieldType/Constants';
import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import useFiledTypeUsages from 'views/logic/fieldactions/ChangeFieldType/hooks/useFiledTypeUsages';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import type { Sort } from 'stores/PaginationTypes';
import type { FieldTypeUsage } from 'views/logic/fieldactions/ChangeFieldType/types';
import useColumnRenderers from 'views/logic/fieldactions/ChangeFieldType/hooks/useColumnRenderers';
import QueryHelper from 'components/common/QueryHelper';

const StyledSelect = styled(Select)`
  width: 400px;
  margin-bottom: 10px;
`;

const Container = styled.div`
  margin-top: 10px;
`;

const ChangeFieldTypeModal = ({ show, onClose, onSubmit, field }: { show: boolean, field: string, onSubmit: () => void, onClose: () => void }) => {
  const [query, setQuery] = useState('');
  const [showDetails, setShowDetails] = useState(false);
  const [activePage, setActivePage] = useState(1);
  const [rotated, setRotated] = useState(false);
  const [newFieldType, setNewFieldType] = useState(null);
  const typeOptions = [{ id: 'number', label: 'Number' }, { id: 'string', label: 'String' }];
  const initialSelection = ['some id'];

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
  const { data: { list, attributes, pagination }, isFirsLoaded } = useFiledTypeUsages(searchParams, field, { enabled: !isLoadingLayoutPreferences });
  const [indexSetSelection, setIndexSetSelection] = useState(initialSelection);
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

  const columnRenderers = useColumnRenderers();

  const onChangeSelection = useCallback((newSelection) => {
    setIndexSetSelection(newSelection);
  }, []);

  const toggleDetailsOpen = useCallback(() => {
    setShowDetails((cur) => !cur);
  }, []);

  if (isLoadingLayoutPreferences || !isFirsLoaded) {
    return <Spinner />;
  }

  return (
    <BootstrapModalForm title={`Change ${field} field type`}
                        submitButtonText="Change field type"
                        onSubmitForm={onSubmit}
                        onCancel={onClose}
                        show={show}>
      <Alert bsStyle="warning">
        <Icon name="info-circle" />&nbsp;
        Text about how bad to change this value and how you ca brake everything
      </Alert>
      <StyledSelect inputId="field_type"
                    valueKey="id"
                    options={typeOptions}
                    value={newFieldType}
                    onChange={(value) => setNewFieldType(value)}
                    placeholder="Select field type"
                    required />
      <Alert bsStyle="info">
        You can choose in which index sets you would like to make the change
      </Alert>
      <Button bsStyle="link" className="btn-text" bsSize="xsmall" onClick={toggleDetailsOpen}>
        <Icon name={`caret-${showDetails ? 'down' : 'right'}`} />&nbsp;
        {showDetails ? 'Hide index sets' : 'Show index sets'}
      </Button>
      {
        showDetails && (
          <Container>
            <PaginatedList onChange={onPageChange}
                           totalItems={pagination?.total}
                           pageSize={layoutConfig.pageSize}
                           activePage={activePage}
                           showPageSizeSelect={false}
                           useQueryParameter={false}>
              <div style={{ marginBottom: 5 }}>
                <SearchForm onSearch={onSearch}
                            queryHelpComponent={<QueryHelper entityName="dashboard" commonFields={['id', 'title', 'description', 'summary']} />}
                            onReset={onResetSearch}
                            query={query}
                            topMargin={0} />
              </div>
              {!list?.length && !query && (
              <NoEntitiesExist>
                No dashboards have been created yet.
              </NoEntitiesExist>
              )}
              {!list?.length && query && (
              <NoSearchResult>No dashboards have been found.</NoSearchResult>
              )}
              {list.length && (
              <EntityDataTable<FieldTypeUsage> activeSort={layoutConfig.sort}
                                               bulkSelection={{
                                                 onChangeSelection,
                                                 initialSelection,
                                               }}
                                               columnDefinitions={attributes}
                                               columnRenderers={columnRenderers}
                                               columnsOrder={DEFAULT_LAYOUT.columnsOrder}
                                               data={list}
                                               onColumnsChange={onColumnsChange}
                                               onPageSizeChange={onPageSizeChange}
                                               onSortChange={onSortChange}
                                               pageSize={layoutConfig.pageSize}
                                               visibleColumns={layoutConfig.displayedAttributes} />
              )}
            </PaginatedList>
          </Container>
        )
      }

      <Input type="checkbox"
             id="rotate"
             name="rotate"
             label="Rotating indexes"
             onChange={() => setRotated((cur) => !cur)}
             checked={rotated} />
    </BootstrapModalForm>
  );
};

export default ChangeFieldTypeModal;
