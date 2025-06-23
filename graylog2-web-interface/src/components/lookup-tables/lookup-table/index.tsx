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

import { lutListElements } from './constants';
import columnRenderers from './column-renderers';
import type { LookupTableEntity } from './types';

import { useFetchLookupTables } from '../hooks/useLookupTablesAPI';

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
  const { fetchPaginatedLookupTables, lookupTablesKeyFn } = useFetchLookupTables();

  return (
    <Row className="content">
      <Col md={12}>
        <PaginatedEntityTable<LookupTableEntity>
          humanName="lookup tables"
          entityActions={null}
          columnsOrder={lutListElements.columnOrder}
          queryHelpComponent={queryHelpComponent}
          tableLayout={lutListElements.defaultLayout}
          fetchEntities={fetchPaginatedLookupTables}
          keyFn={lookupTablesKeyFn}
          actionsCellWidth={0}
          entityAttributesAreCamelCase={false}
          columnRenderers={columnRenderers}
        />
      </Col>
    </Row>
  );
}

export default LookupTableList;
