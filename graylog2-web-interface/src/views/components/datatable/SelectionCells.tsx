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

import RowCheckbox from 'components/common/EntityDataTable/RowCheckbox';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import BulkSelectCell from 'components/common/message/messagetable/BulkSelectCell';

import TableHeaderCell from './TableHeaderCell';

type SelectAllHeaderCellProps = {
  rowIds: Array<string>;
};

export const SelectAllHeaderCell = ({ rowIds }: SelectAllHeaderCellProps) => {
  const { isAllRowsSelected, isSomeRowsSelected, setSelectedEntities } = useSelectedEntities();
  const title = `${isAllRowsSelected ? 'Deselect' : 'Select'} all rows`;

  return (
    <TableHeaderCell $noMinWidth>
      <RowCheckbox
        checked={isAllRowsSelected}
        indeterminate={isSomeRowsSelected}
        onChange={() => setSelectedEntities(isAllRowsSelected ? [] : rowIds)}
        title={title}
        aria-label={title}
      />
    </TableHeaderCell>
  );
};

type SelectRowCellProps = {
  rowId: string;
};

export const SelectRowCell = ({ rowId }: SelectRowCellProps) => {
  const { selectedEntities, toggleEntitySelect } = useSelectedEntities();
  const isSelected = selectedEntities.includes(rowId);
  const title = `${isSelected ? 'Deselect' : 'Select'} row`;

  return (
    <BulkSelectCell>
      <RowCheckbox checked={isSelected} onChange={() => toggleEntitySelect(rowId)} title={title} aria-label={title} />
    </BulkSelectCell>
  );
};
