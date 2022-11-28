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
import { useState, useRef } from 'react';

import { Checkbox, Button, Popover } from 'components/bootstrap';
import type { Column } from 'components/common/EntityDataTable/types';
import { Icon, Portal } from 'components/common';
import TextOverflowEllipsis from 'components/common/TextOverflowEllipsis';

const StyledPopover = styled(Popover)`
  margin-right: 5px;
  
  .popover-content {
    max-width: 180px;
    padding: 5px 10px;
  }
`;

const ColumnsVisibilityCheckbox = styled(Checkbox)`
  display: inline-block;
  vertical-align: sub;
  margin: 0;
`;

const ListItem = styled.div`
  padding: 3px 0;
  cursor: pointer;
  display: flex;
`;

const ItemTitle = styled(TextOverflowEllipsis)`
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
      <ColumnsVisibilityCheckbox checked={isSelected} />
      <ItemTitle>{column.title}</ItemTitle>
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

  return (
    <>
      <Button onClick={toggleColumnSelect} ref={buttonRef} bsSize="small">
        <Icon name="gear" /> Columns
      </Button>

      {showSelect && (
        <Portal>
          <Overlay target={buttonRef.current} placement="bottom" show onHide={toggleColumnSelect} rootClose>
            <StyledPopover id="create-filter-search-popover">
              {allColumns.map((column) => (
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
