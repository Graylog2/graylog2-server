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
import { useMemo } from 'react';

import { Checkbox, DropdownButton } from 'components/bootstrap';
import type { Column } from 'components/common/EntityDataTable/types';
import TextOverflowEllipsis from 'components/common/TextOverflowEllipsis';
import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import MenuItem from 'components/bootstrap/MenuItem';

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

const ColumnListItem = ({
  allColumns,
  column,
  onClick,
  selectedColumns,
}: {
  allColumns: Array<Column>
  column: Column,
  onClick: (selectedColumns: Array<string>) => void,
  selectedColumns: Array<string>,
}) => {
  const isSelected = selectedColumns.includes(column.id);

  const toggleVisibility = () => {
    const newAttributes = allColumns.reduce((collection, attr) => {
      const isCurAttr = column.id === attr.id;

      if ((isCurAttr && !isSelected) || (!isCurAttr && selectedColumns.includes(attr.id))) {
        return [...collection, attr.id];
      }

      return collection;
    }, []);

    onClick(newAttributes);
  };

  return (
    <MenuItem onSelect={toggleVisibility} title={`${isSelected ? 'Hide' : 'Show'} ${column.title}`}>
      <ListItemInner>
        <ColumnCheckbox checked={isSelected} onChange={toggleVisibility} />
        <ColumnTitle>{column.title}</ColumnTitle>
      </ListItemInner>
    </MenuItem>
  );
};

type Props = {
  allColumns: Array<Column>
  onChange: (attributes: Array<string>) => void,
  selectedColumns: Array<string>,
}

const ColumnsVisibilitySelect = ({ onChange, selectedColumns, allColumns }: Props) => {
  const sortedColumns = useMemo(
    () => allColumns.sort((col1, col2) => (naturalSort(col1.title, col2.title))),
    [allColumns],
  );

  return (
    <StyledDropdownButton title="Columns"
                          bsSize="small"
                          pullRight
                          aria-label="Configure visible columns"
                          id="columns-visibility-select"
                          bsStyle="default"
                          closeOnItemClick={false}>
      {sortedColumns.map((column) => (
        <ColumnListItem column={column}
                        onClick={onChange}
                        key={column.id}
                        allColumns={allColumns}
                        selectedColumns={selectedColumns} />
      ))}
    </StyledDropdownButton>
  );
};

export default ColumnsVisibilitySelect;
