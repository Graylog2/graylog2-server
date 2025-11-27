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

import { useMemo } from 'react';

import { BULK_SELECT_COL_ID, ACTIONS_COL_ID } from 'components/common/EntityDataTable/Constants';
import type { ColumnPreferences } from 'components/common/EntityDataTable/types';

const getVisibleAttributeColumns = (
  defaultDisplayedColumns: Array<string>,
  userColumnPreferences: ColumnPreferences | undefined = {},
) => {
  const visible = new Set(
    Object.entries(userColumnPreferences)
      .filter(([, { status }]) => status === 'show')
      .map(([attr]) => attr),
  );

  // Add default columns, which are not explicitly hidden
  defaultDisplayedColumns.forEach((attr) => {
    if (!userColumnPreferences[attr]) {
      visible.add(attr);
    }
  });

  return visible;
};
const useVisibleColumnOrder = (
  columnPreferences: ColumnPreferences | undefined,
  attributeColumnsOrder: Array<string>,
  defaultDisplayedColumns: Array<string>,
  displayActionsCol: boolean,
  displayBulkSelectCol: boolean,
) =>
  useMemo(() => {
    const visibleAttributeColumns = getVisibleAttributeColumns(defaultDisplayedColumns, columnPreferences);
    // Core order: visible attributes in the order defined by attributeColumnsOrder
    const coreOrder = attributeColumnsOrder.filter((id) => visibleAttributeColumns.has(id));
    // Additional: visible attributes not in attributeColumnsOrder
    const additionalVisible = [...visibleAttributeColumns].filter((id) => !attributeColumnsOrder.includes(id));

    return [
      ...(displayBulkSelectCol ? [BULK_SELECT_COL_ID] : []),
      ...coreOrder,
      ...additionalVisible,
      ...(displayActionsCol ? [ACTIONS_COL_ID] : []),
    ];
  }, [columnPreferences, defaultDisplayedColumns, attributeColumnsOrder, displayBulkSelectCol, displayActionsCol]);

export default useVisibleColumnOrder;
