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
import { useMemo } from 'react';

import { defaultCompare } from 'logic/DefaultCompare';
import Select from 'components/common/Select';
import type { SelectRef } from 'components/common/Select/Select';

type Props = {
  ariaLabel?: string,
  autoFocus?: boolean,
  allowCreate?: boolean,
  className?: string,
  clearable?: boolean,
  columns: Array<string>,
  excludedColumns?: Array<string>,
  id: string,
  menuPortalTarget?: HTMLElement,
  name: string,
  onChange: (columnName: string) => void,
  onMenuClose?: () => void,
  openMenuOnFocus?: boolean,
  persistSelection?: boolean,
  placeholder?: string,
  selectRef?: SelectRef,
  value: string | undefined,
  columnTitle: (column: string) => string,
}

const ColumnSelect = ({
  ariaLabel,
  autoFocus,
  allowCreate = false,
  className,
  clearable = false,
  columns,
  columnTitle,
  id,
  excludedColumns = [],
  menuPortalTarget,
  name,
  onChange,
  onMenuClose,
  openMenuOnFocus,
  persistSelection,
  placeholder,
  selectRef,
  value,
}: Props) => {
  const columnOptions = useMemo(() => columns
    .filter((column) => !excludedColumns.includes(column))
    .map((column) => ({
      value: column,
      label: columnTitle(column),
    })).sort(({ label: label1 }, { label: label2 }) => defaultCompare(label1, label2)),
  [columnTitle, columns, excludedColumns]);

  return (
    <Select options={columnOptions}
            inputId={`select-${id}`}
            forwardedRef={selectRef}
            allowCreate={allowCreate}
            className={className}
            onMenuClose={onMenuClose}
            openMenuOnFocus={openMenuOnFocus}
            persistSelection={persistSelection}
            clearable={clearable}
            placeholder={placeholder}
            name={name}
            size="small"
            value={value}
            aria-label={ariaLabel}
            autoFocus={autoFocus}
            menuPortalTarget={menuPortalTarget}
            onChange={onChange} />
  );
};

export default ColumnSelect;
