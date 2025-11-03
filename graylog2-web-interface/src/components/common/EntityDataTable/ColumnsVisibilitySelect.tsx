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
import styled from 'styled-components';
import * as React from 'react';
import type { Column, Table } from '@tanstack/react-table';

import { defaultCompare } from 'logic/DefaultCompare';
import { Checkbox, DropdownButton, MenuItem } from 'components/bootstrap';
import TextOverflowEllipsis from 'components/common/TextOverflowEllipsis';
import type { EntityBase, ColumnMeta } from 'components/common/EntityDataTable/types';

const StyledDropdownButton = styled(DropdownButton)`
  ~ .dropdown-menu {
    min-width: auto;
    max-width: 180px;
  }
`;

const ColumnCheckbox = styled(Checkbox)`
  &.checkbox {
    margin: 0 5px 0 0;

    label {
      display: flex;
      align-items: center;
      padding: 0;

      input {
        margin: 0;
        position: relative;
      }
    }
  }
`;

const ListItemInner = styled.div`
  display: flex;
`;

const ColumnTitle = styled(TextOverflowEllipsis)`
  display: inline;
`;

const colLabel = <Entity extends EntityBase>(column: Column<Entity>) =>
  (column.columnDef.meta as ColumnMeta)?.label ?? column.id;

const ColumnListItem = <Entity extends EntityBase>({ column }: { column: Column<Entity> }) => {
  const isSelected = column.getIsVisible();
  const toggleVisibility = () => column.toggleVisibility();
  const label = colLabel(column);

  return (
    <MenuItem onSelect={toggleVisibility} title={`${isSelected ? 'Hide' : 'Show'} ${label}`}>
      <ListItemInner>
        <ColumnCheckbox checked={isSelected} onChange={toggleVisibility} />
        <ColumnTitle>{label}</ColumnTitle>
      </ListItemInner>
    </MenuItem>
  );
};

type Props<Entity> = {
  table: Table<Entity>;
};

const ColumnsVisibilitySelect = <Entity extends EntityBase>({ table }: Props<Entity>) => (
  <StyledDropdownButton
    title="Columns"
    bsSize="small"
    pullRight
    aria-label="Configure visible columns"
    id="columns-visibility-select"
    bsStyle="default"
    closeOnItemClick={false}>
    {table
      .getAllLeafColumns()
      .filter((column) => column.getCanHide())
      .sort((col1, col2) => defaultCompare(colLabel<Entity>(col1), colLabel<Entity>(col2)))
      .map((column) => (
        <ColumnListItem column={column} key={column.id} />
      ))}
  </StyledDropdownButton>
);
export default ColumnsVisibilitySelect;
