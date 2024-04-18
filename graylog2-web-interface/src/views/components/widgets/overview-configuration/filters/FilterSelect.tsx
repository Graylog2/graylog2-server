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
import { useMemo, useState, useRef } from 'react';
import styled from 'styled-components';

import { Select } from 'components/common';
import { defaultCompare } from 'logic/DefaultCompare';
import { Menu } from 'components/bootstrap';
import type { FilterComponents, Filter } from 'views/components/widgets/overview-configuration/filters/types';

const SELECT_TITLE = 'Configure a new filter';

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
  selectedFilters: Array<Filter>,
}

const FilterSelect = ({ filterComponents, columnTitle, onCreate, selectedFilters }: Props) => {
  const container = useRef(null);
  const [open, setOpen] = useState(false);
  const [selectedColumn, setSelectedColumn] = useState<string>(null);
  const [createValue, setCreateValue] = React.useState<string>();
  const filterComponent = selectedColumn ? filterComponents.find(({ attribute }) => attribute === selectedColumn) : undefined;
  const selectedValues = selectedColumn ? selectedFilters.find(({ field }) => field === selectedColumn)?.value : [];

  const filterOptions = useMemo(() => (
    filterComponents
      .map((filtComp) => ({
        value: filtComp.attribute,
        label: columnTitle(filtComp.attribute),
        disabled: !filtComp.allowMultipleValues && !!selectedFilters.find(({ field }) => field === filtComp.attribute),
      }))
      .sort(({ label: label1 }, { label: label2 }) => defaultCompare(label1, label2))
  ), [columnTitle, filterComponents, selectedFilters]);

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

  const onChange = (newValue: unknown, shouldSubmit = true) => {
    const normalizedValue = filterComponent?.valueForConfig?.(newValue) ?? newValue as string;

    if (shouldSubmit) {
      onSubmit(selectedColumn, normalizedValue);
    } else {
      setCreateValue(normalizedValue);
    }
  };

  return (
    <Container ref={container}>
      <Select id="filter-select"
              placeholder={SELECT_TITLE}
              aria-label={SELECT_TITLE}
              options={filterOptions}
              matchProp="label"
              menuPortalTarget={document.body}
              clearable={false}
              size="small"
              onChange={(selectedCol) => onSelectColumn(selectedCol)}
              value={selectedColumn} />
      <Menu position="bottom-start" withinPortal onClose={onClose} opened={open} width={container?.current?.offsetWidth}>
        <Menu.Target>
          <HiddenButton type="button" />
        </Menu.Target>
        <Menu.Dropdown>
          {filterComponent?.configuration(selectedValues ?? [], filterComponent?.valueFromConfig?.(createValue) ?? createValue, onChange)}
        </Menu.Dropdown>
      </Menu>
    </Container>
  );
};

export default FilterSelect;
