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
import { BULK_SELECT_COL_ID } from 'components/common/EntityDataTable/Constants';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';

const BulkSelectHeader = <Entity extends EntityBase>({ table }: { table: Table<Entity> }) => {
  const { isSomeRowsSelected, isAllRowsSelected } = useSelectedEntities();
  const title = `${isAllRowsSelected ? 'Deselect' : 'Select'} all visible entities`;

  return (
    <RowCheckbox
      onChange={table.getToggleAllRowsSelectedHandler()}
      checked={isAllRowsSelected}
      indeterminate={isSomeRowsSelected}
      title={title}
      disabled={!table.options?.data?.length}
      aria-label={title}
    />
  );
};

const BulkSelectCell = <Entity extends EntityBase>({ row }: { row: Row<Entity> }) => {
  const { selectedEntities } = useSelectedEntities();

  return (
    <RowCheckbox
      onChange={row.getToggleSelectedHandler()}
      title={`${row.getIsSelected() ? 'Deselect' : 'Select'} entity`}
      checked={selectedEntities.includes(row.id)}
      disabled={!row.getCanSelect()}
    />
  );
};

const useBulkSelectColumnDefinition = <Entity extends EntityBase>(displayBulkSelectCol: boolean, colWidth: number) => {
  const columnHelper = createColumnHelper<Entity>();

  return useMemo(
    () =>
      displayBulkSelectCol
        ? columnHelper.display({
            id: BULK_SELECT_COL_ID,
            size: colWidth,
            header: BulkSelectHeader<Entity>,
            enableHiding: false,
            cell: BulkSelectCell<Entity>,
            enableResizing: false,
          })
        : null,
    [displayBulkSelectCol, columnHelper, colWidth],
  );
};

export default useBulkSelectColumnDefinition;
