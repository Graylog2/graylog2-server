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

import PaginatedEntityTable from 'components/common/PaginatedEntityTable';
import QueryHelper from 'components/common/QueryHelper';
import type { LookupTable } from 'logic/lookup-tables/types';

import { lutListElements } from './constants';

function LookupTableList() {
  return (
    <PaginatedEntityTable<LookupTable & { id: string }>
      humanName="lookup tables"
      entityActions={null}
      columnsOrder={lutListElements.columnOrder}
      queryHelpComponent={<QueryHelper entityName="lookup tables" />}
      tableLayout={lutListElements.defaultLayout}
      fetchEntities={handleFetchPacks}
      keyFn={packEntitiesKeyFn}
      actionsCellWidth={0}
      entityAttributesAreCamelCase={false}
      columnRenderers={columnRenderers}
      bulkSelection={bulkSelection}
      expandedSectionsRenderer={expandedSections}
    />
  );
}

export default LookupTableList;
