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

import { Row, Col } from 'components/bootstrap';
import PaginatedEntityTable from 'components/common/PaginatedEntityTable';
import QueryHelper from 'components/common/QueryHelper';
import { ErrorsProvider } from 'components/lookup-tables/contexts/ErrorsContext';
import ErrorsConsumer from 'components/lookup-tables/lookup-table-list/errors-consumer';
import { useFetchDataAdapters } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import { ModalProvider } from 'components/lookup-tables/contexts/ModalContext';
import LUTModals from 'components/lookup-tables/LUTModals';
import type { SearchParams, Attribute } from 'stores/PaginationTypes';
import type { DataAdapterEntity } from 'components/lookup-tables/types';

import { adapterListElements } from './constants';
import columnRenderers from './column-renderers';
import useActions from './use-actions';

const queryHelpComponent = (
  <QueryHelper
    entityName="data adapter"
    commonFields={['id', 'title', 'name', 'description']}
    example={
      <p>
        searching without a field name matches against the <code>title</code> field:
        <br />
        <kbd>geoip</kbd> <br />
        is the same as
        <br />
        <kbd>title:geoip</kbd>
      </p>
    }
  />
);

function DataAdapterList() {
  const [{ adapterNames }, setNames] = React.useState<{
    adapterNames?: Array<string>;
  }>({ adapterNames: undefined });
  const { fetchPaginatedDataAdapters, dataAdaptersKeyFn } = useFetchDataAdapters();
  const { renderActions } = useActions();

  const handleFetchAdapters = React.useCallback(
    async (searchParams: SearchParams) => {
      const resp = await fetchPaginatedDataAdapters(searchParams);

      const overrides: Record<string, Partial<Attribute>> = {
        title: { sortable: true },
        name: { sortable: true },
        description: { sortable: false },
      };

      const expectedIds = adapterListElements.defaultLayout.defaultDisplayedAttributes;

      const attrMap = new Map<string, Attribute>();

      (resp.attributes ?? []).forEach((attr) => {
        const override = overrides[attr.id] ?? {};
        attrMap.set(attr.id, { ...attr, ...override });
      });

      expectedIds.forEach((id) => {
        if (!attrMap.has(id)) {
          const override = overrides[id] ?? {};
          attrMap.set(id, {
            id,
            title: id.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase()),
            type: 'STRING',
            ...override,
          });
        }
      });

      setNames({
        adapterNames: resp.list.map((adapter: DataAdapterEntity) => adapter.name),
      });

      return Promise.resolve({
        ...resp,
        attributes: Array.from(attrMap.values()),
      });
    },
    [fetchPaginatedDataAdapters],
  );

  return (
    <ModalProvider>
      <ErrorsProvider>
        <ErrorsConsumer adapterNames={adapterNames} />
        <Row className="content">
          <Col md={12}>
            <PaginatedEntityTable<DataAdapterEntity>
              humanName="data adapter"
              entityActions={renderActions}
              columnsOrder={adapterListElements.columnOrder}
              queryHelpComponent={queryHelpComponent}
              tableLayout={adapterListElements.defaultLayout}
              fetchEntities={handleFetchAdapters}
              keyFn={dataAdaptersKeyFn}
              actionsCellWidth={100}
              entityAttributesAreCamelCase={false}
              columnRenderers={columnRenderers}
            />
          </Col>
        </Row>
        <LUTModals />
      </ErrorsProvider>
    </ModalProvider>
  );
}

export default DataAdapterList;
