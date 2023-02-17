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
import { Overlay } from 'react-overlays';
import { useState, useRef, useMemo } from 'react';

import { Checkbox, Button, Popover } from 'components/bootstrap';
import type { Column } from 'components/common/EntityDataTable/types';
import { Icon, Portal } from 'components/common';
import TextOverflowEllipsis from 'components/common/TextOverflowEllipsis';
import { defaultCompare as naturalSort } from 'logic/DefaultCompare';

const StyledPopover = styled(Popover)`
  margin-right: 5px;
  
  .popover-content {
    max-width: 180px;
    padding: 5px 10px;
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

const ListItem = styled.div`
  padding: 3px 0;
  cursor: pointer;
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
    <ListItem role="menuitem" onClick={toggleVisibility} title={`${isSelected ? 'Hide' : 'Show'} ${column.title}`}>
      <ColumnCheckbox checked={isSelected} onChange={toggleVisibility} />
      <ColumnTitle>{column.title}</ColumnTitle>
    </ListItem>
  );
};

type Props = {
  allColumns: Array<Column>
  onChange: (attributes: Array<string>) => void,
  selectedColumns: Array<string>,
}

const ColumnsVisibilitySelect = ({ onChange, selectedColumns, allColumns }: Props) => {
  const buttonRef = useRef();
  const [showSelect, setShowSelect] = useState(false);

  const toggleColumnSelect = () => {
    setShowSelect((cur) => !cur);
  };

  const sortedColumns = useMemo(
    () => allColumns.sort((col1, col2) => (naturalSort(col1.title, col2.title))),
    [allColumns],
  );

  return (
    <>
      <Button onClick={toggleColumnSelect} ref={buttonRef} bsSize="small" title="Select columns to display">
        Columns <span className="caret" />
      </Button>

      {showSelect && (
        <Portal>
          <Overlay target={buttonRef.current} placement="bottom" show onHide={toggleColumnSelect} rootClose>
            <StyledPopover id="columns-visibility-select">
              {sortedColumns.map((column) => (
                <ColumnListItem column={column}
                                onClick={onChange}
                                key={column.id}
                                allColumns={allColumns}
                                selectedColumns={selectedColumns} />
              ))}
            </StyledPopover>
          </Overlay>
        </Portal>
      )}
    </>
  );
};

export default ColumnsVisibilitySelect;
