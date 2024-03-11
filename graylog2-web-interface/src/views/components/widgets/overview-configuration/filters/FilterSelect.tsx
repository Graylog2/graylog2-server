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
import { useMemo, useState } from 'react';
import styled from 'styled-components';

import { Select } from 'components/common';
import { defaultCompare } from 'logic/DefaultCompare';
import { Menu } from 'components/bootstrap';
import { FilterComponents } from 'views/components/widgets/overview-configuration/filters/types';


const Container = styled.div`
  display: flex;
  flex-direction: column;
`;

const HiddenButton = styled.button`
  visibility: hidden;
  height: 0;
  padding: 0;
  border: 0;
`;

type Props = {
  filterComponents: FilterComponents,
  columnTitle: (column: string) => string,
  onCreate: (column: string, value: string) => void,
}

const FilterSelect = ({ filterComponents, columnTitle, onCreate }: Props) => {
  const [open, setOpen] = useState(false);
  const [selectedColumn, setSelectedColumn] = useState<string>(null);
  const [createValue, setCreateValue] = React.useState<string>();
  const filterComponent = selectedColumn ? filterComponents[selectedColumn] : undefined;

  const filterOptions = useMemo(() => (
    Object.keys(filterComponents)
      .map((col) => ({ value: col, label: columnTitle(col) }))
      .sort(({ label: label1 }, { label: label2 }) => defaultCompare(label1, label2))
  ), [columnTitle, filterComponents]);

  const onSelectColumn = (selectedCol: string) => {
    setSelectedColumn(selectedCol);
    setOpen(true);
  };

  const onSubmit = (column: string, val: string | undefined) => {
    if (val) {
      onCreate(column, val);
    }

    setOpen(false);
    setSelectedColumn(null);
    setCreateValue(null);
  };

  const onClose = () => {
    onSubmit(selectedColumn, createValue);
  };

  const onChange = (newValue: unknown, shouldSubmit = false) => {
    const normalizedValue = filterComponent?.valueForConfig?.(newValue) ?? newValue as string;

    if (shouldSubmit) {
      onSubmit(selectedColumn, normalizedValue);
    } else {
      setCreateValue(filterComponent?.valueForConfig?.(newValue) ?? newValue as string);
    }
  };

  return (
    <Container>
      <Select id="filter-select"
              placeholder="Configure a new filter"
              options={filterOptions}
              persistSelection={false}
              matchProp="label"
              menuPortalTarget={document.body}
              clearable={false}
              size="small"
              onChange={(selectedCol) => onSelectColumn(selectedCol)}
              value={null} />
      <Menu position="bottom-start" withinPortal onClose={onClose} opened={open}>
        <Menu.Target>
          <HiddenButton type="button" />
        </Menu.Target>
        <Menu.Dropdown>
          {filterComponent?.configuration(filterComponent?.valueFromConfig?.(createValue) ?? createValue, onChange)}
        </Menu.Dropdown>
      </Menu>
    </Container>
  );
};

export default FilterSelect;
