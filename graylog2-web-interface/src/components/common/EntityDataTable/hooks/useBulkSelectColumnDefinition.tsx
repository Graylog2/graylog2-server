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

import type { Table, Row } from '@tanstack/react-table';
import { createColumnHelper } from '@tanstack/react-table';
import * as React from 'react';
import { useMemo } from 'react';

import RowCheckbox from 'components/common/EntityDataTable/RowCheckbox';
import type { EntityBase } from 'components/common/EntityDataTable/types';
import { BULK_SELECT_COL_ID, BULK_SELECT_COLUMN_WIDTH } from 'components/common/EntityDataTable/Constants';

const BulkSelectHeader = <Entity extends EntityBase>({ table }: { table: Table<Entity> }) => {
  const checked = table.getIsAllRowsSelected();
  const title = `${checked ? 'Deselect' : 'Select'} all visible entities`;

  return (
    <RowCheckbox
      onChange={table.getToggleAllRowsSelectedHandler()}
      checked={checked}
      indeterminate={table.getIsSomeRowsSelected()}
      title={title}
      disabled={!table.options?.data?.length}
      aria-label={title}
    />
  );
};

const BulkSelectCell = <Entity extends EntityBase>({ row }: { row: Row<Entity> }) => (
  <RowCheckbox
    onChange={row.getToggleSelectedHandler()}
    title={`${row.getIsSelected() ? 'Deselect' : 'Select'} entity`}
    checked={row.getIsSelected()}
    disabled={!row.getCanSelect()}
  />
);

const useBulkSelectColumnDefinition = <Entity extends EntityBase>(displayBulkSelectCol: boolean) => {
  const columnHelper = createColumnHelper<Entity>();

  return useMemo(
    () =>
      displayBulkSelectCol
        ? columnHelper.display({
            id: BULK_SELECT_COL_ID,
            size: BULK_SELECT_COLUMN_WIDTH,
            header: BulkSelectHeader<Entity>,
            enableHiding: false,
            cell: BulkSelectCell<Entity>,
            enableResizing: false,
          })
        : null,
    [displayBulkSelectCol, columnHelper],
  );
};

export default useBulkSelectColumnDefinition;
