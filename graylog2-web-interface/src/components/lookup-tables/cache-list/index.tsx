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
import { useFetchCaches } from 'components/lookup-tables/hooks/useLookupTablesAPI';

import { cacheListElements } from './constants';
import columnRenderers from './column-renderers';
import type { CacheEntity } from './types';

const queryHelpComponent = (
  <QueryHelper
    entityName="cache"
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
  const { fetchPaginatedCaches, cachesKeyFn } = useFetchCaches();

  return (
    <ErrorsProvider>
      <Row className="content">
        <Col md={12}>
          <PaginatedEntityTable<CacheEntity>
            humanName="caches"
            entityActions={null}
            columnsOrder={cacheListElements.columnOrder}
            queryHelpComponent={queryHelpComponent}
            tableLayout={cacheListElements.defaultLayout}
            fetchEntities={fetchPaginatedCaches}
            keyFn={cachesKeyFn}
            actionsCellWidth={0}
            entityAttributesAreCamelCase={false}
            columnRenderers={columnRenderers}
          />
        </Col>
      </Row>
    </ErrorsProvider>
  );
}

export default LookupTableList;
