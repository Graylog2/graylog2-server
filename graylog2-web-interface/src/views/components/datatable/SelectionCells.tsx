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
import styled from 'styled-components';

import RowCheckbox from 'components/common/EntityDataTable/RowCheckbox';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';

import TableHeaderCell from './TableHeaderCell';
import { rowSelectionLabel } from './rowSelection';

const SelectionHeader = styled(TableHeaderCell)`
  && {
    width: 1px;
  }
`;

const SelectionData = styled.td`
  && {
    width: 1px;
  }
`;

type SelectAllHeaderCellProps = {
  rowIds: Array<string>;
};

export const SelectAllHeaderCell = ({ rowIds }: SelectAllHeaderCellProps) => {
  const { isAllRowsSelected, isSomeRowsSelected, setSelectedEntities } = useSelectedEntities();
  const label = isAllRowsSelected ? 'Deselect all rows' : 'Select all rows';

  return (
    <SelectionHeader>
      <RowCheckbox
        checked={isAllRowsSelected}
        indeterminate={isSomeRowsSelected}
        onChange={() => setSelectedEntities(isAllRowsSelected ? [] : rowIds)}
        title={label}
        aria-label={label}
      />
    </SelectionHeader>
  );
};

type SelectRowCellProps = {
  rowId: string;
};

export const SelectRowCell = ({ rowId }: SelectRowCellProps) => {
  const { selectedEntities, toggleEntitySelect } = useSelectedEntities();
  const label = rowSelectionLabel(rowId);

  return (
    <SelectionData>
      <RowCheckbox
        checked={selectedEntities.includes(rowId)}
        onChange={() => toggleEntitySelect(rowId)}
        title={label}
        aria-label={label}
      />
    </SelectionData>
  );
};
