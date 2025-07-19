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
import { useFetchLookupTables } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import { ModalProvider } from 'components/lookup-tables/contexts/ModalContext';
import LUTModals from 'components/lookup-tables/LUTModals';
import type { SearchParams } from 'stores/PaginationTypes';
import type { LookupTableCache, LookupTableAdapter } from 'logic/lookup-tables/types';
import type { LookupTableEntity } from 'components/lookup-tables/types';

import { lutListElements } from './constants';
import columnRenderers from './column-renderers';
import ErrorsConsumer from './errors-consumer';
import useActions from './use-actions';

const queryHelpComponent = (
  <QueryHelper
    entityName="lookup table"
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

function LookupTableList() {
  const [{ lutNames, cacheNames, adapterNames }, setNames] = React.useState<{
    lutNames?: Array<string>;
    cacheNames?: Array<string>;
    adapterNames?: Array<string>;
  }>({ lutNames: undefined, cacheNames: undefined, adapterNames: undefined });
  const { fetchPaginatedLookupTables, lookupTablesKeyFn } = useFetchLookupTables();
  const { renderActions } = useActions();

  const handleFetchTables = React.useCallback(
    async (searchParams: SearchParams) => {
      const resp = await fetchPaginatedLookupTables(searchParams);

      setNames({
        lutNames: resp.list.map((lut: LookupTableEntity) => lut.name),
        cacheNames: Object.values(resp.meta.caches).map((cache: LookupTableCache) => cache.name),
        adapterNames: Object.values(resp.meta.adapters).map((adapter: LookupTableAdapter) => adapter.name),
      });

      return Promise.resolve(resp);
    },
    [fetchPaginatedLookupTables, setNames],
  );

  return (
    <ModalProvider>
      <ErrorsProvider>
        <ErrorsConsumer lutNames={lutNames} cacheNames={cacheNames} adapterNames={adapterNames} />
        <Row className="content">
          <Col md={12}>
            <PaginatedEntityTable<LookupTableEntity>
              humanName="lookup tables"
              entityActions={renderActions}
              columnsOrder={lutListElements.columnOrder}
              queryHelpComponent={queryHelpComponent}
              tableLayout={lutListElements.defaultLayout}
              fetchEntities={handleFetchTables}
              keyFn={lookupTablesKeyFn}
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

export default LookupTableList;
