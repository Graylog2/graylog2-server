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

import { BULK_SELECT_COL_ID, ACTIONS_COL_ID, ATTRIBUTE_STATUS } from 'components/common/EntityDataTable/Constants';
import type { ColumnPreferences } from 'components/common/EntityDataTable/types';

const getVisibleAttributeColumns = (
  defaultDisplayedColumns: Array<string>,
  userColumnPreferences: ColumnPreferences | undefined = {},
) => {
  const userSelection = new Set(
    Object.entries(userColumnPreferences)
      .filter(([, { status }]) => status === ATTRIBUTE_STATUS.show)
      .map(([attr]) => attr),
  );

  if (userSelection.size > 0) {
    return userSelection;
  }

  return new Set(defaultDisplayedColumns);
};
const useVisibleColumnOrder = (
  columnPreferences: ColumnPreferences | undefined,
  attributeColumnOrder: Array<string>,
  defaultDisplayedColumns: Array<string>,
  displayBulkSelectCol: boolean,
) =>
  useMemo(() => {
    const visibleAttributeColumns = getVisibleAttributeColumns(defaultDisplayedColumns, columnPreferences);
    // Core order: visible attributes in the order defined by attributeColumnOrder
    const coreOrder = attributeColumnOrder.filter((id) => visibleAttributeColumns.has(id));
    // Additional: visible attributes not in attributeColumnOrder
    const additionalVisible = [...visibleAttributeColumns].filter((id) => !attributeColumnOrder.includes(id));

    // Keep actions as the trailing column even when there are no row actions.
    // It doubles as the "tail" column to consume leftover width for fully-static layouts.
    return [...(displayBulkSelectCol ? [BULK_SELECT_COL_ID] : []), ...coreOrder, ...additionalVisible, ACTIONS_COL_ID];
  }, [columnPreferences, defaultDisplayedColumns, attributeColumnOrder, displayBulkSelectCol]);

export default useVisibleColumnOrder;
