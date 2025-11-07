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

const useVisibleColumnOrder = (
  columnPreferences: ColumnPreferences,
  attributeColumnsOder: Array<string>,
  displayActionsCol: boolean,
  displayBulkSelectCol: boolean,
) =>
  useMemo(() => {
    const visibleAttributeColumns = Object.keys(columnPreferences).filter(
      (key) => columnPreferences[key].status === 'show',
    );

    const visibleAttributesSet = new Set(visibleAttributeColumns);
    // Core order: visible attributes in the order defined by attributeColumnsOder
    const coreOrder = attributeColumnsOder.filter((id) => visibleAttributesSet.has(id));
    // Additional: visible attributes not in attributeColumnsOder
    const additionalVisible = visibleAttributeColumns.filter((id) => !attributeColumnsOder.includes(id));

    return [
      ...(displayBulkSelectCol ? [BULK_SELECT_COL_ID] : []),
      ...coreOrder,
      ...additionalVisible,
      ...(displayActionsCol ? [ACTIONS_COL_ID] : []),
    ];
  }, [columnPreferences, attributeColumnsOder, displayBulkSelectCol, displayActionsCol]);

export default useVisibleColumnOrder;
