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
import { useState } from 'react';

import { useIsInteractiveMode } from 'views/components/contexts/InteractiveContext';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';

import type { Row } from './rowSelection';
import { rowSelectionKey, rowSelectionLabel, selectableFieldsFor, selectedValuesByField } from './rowSelection';

const useRowSelection = (
  rows: Array<Row>,
  rowFieldNames: Array<string>,
  selectableFieldNames: Array<string>,
  types: FieldTypeMappingsList,
) => {
  const interactive = useIsInteractiveMode();
  const [selection, setSelection] = useState<Array<string>>([]);
  const selectableFields = selectableFieldsFor(selectableFieldNames, types, interactive);
  const isSelectable = selectableFields.length > 0;
  const rowKeys = isSelectable ? rows.map((row) => rowSelectionKey(row, rowFieldNames)) : [];
  // Ignores selected rows which are no longer part of the result.
  const selectedKeys = rowKeys.filter((key) => selection.includes(key));
  const allSelected = rowKeys.length > 0 && selectedKeys.length === rowKeys.length;

  const toggleRow = (key: string) =>
    setSelection((cur) => (cur.includes(key) ? cur.filter((curKey) => curKey !== key) : [...cur, key]));

  const selectionFor = (row: Row) => {
    if (!isSelectable) {
      return undefined;
    }

    const key = rowSelectionKey(row, rowFieldNames);

    return {
      checked: selectedKeys.includes(key),
      onChange: () => toggleRow(key),
      label: rowSelectionLabel(row, rowFieldNames),
    };
  };

  return {
    selectAll: isSelectable
      ? {
          checked: allSelected,
          indeterminate: selectedKeys.length > 0 && !allSelected,
          onChange: () => setSelection(allSelected ? [] : rowKeys),
        }
      : undefined,
    selectionFor,
    selectedRowsCount: selectedKeys.length,
    selectedValuesByField: selectedValuesByField(rows, rowFieldNames, selectableFields, selectedKeys, types),
    clearSelection: () => setSelection([]),
  };
};

export default useRowSelection;
