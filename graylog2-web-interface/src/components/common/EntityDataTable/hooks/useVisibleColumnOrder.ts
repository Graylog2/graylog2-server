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

const useVisibleColumnOrder = (
  visibleAttributeColumns: Array<string>,
  attributeColumnsOder: Array<string>,
  displayActionsCol: boolean,
  displayBulkSelectCol: boolean,
) =>
  useMemo(() => {
    const visibleSet = new Set(visibleAttributeColumns);
    // Core order: visible attributes in the order defined by attributeColumnsOder
    const coreOrder = attributeColumnsOder.filter((id) => visibleSet.has(id));
    // Additional: visible attributes not in attributeColumnsOder
    const additionalVisible = visibleAttributeColumns.filter((id) => !attributeColumnsOder.includes(id));

    return [
      ...(displayBulkSelectCol ? [BULK_SELECT_COL_ID] : []),
      ...coreOrder,
      ...additionalVisible,
      ...(displayActionsCol ? [ACTIONS_COL_ID] : []),
    ];
  }, [visibleAttributeColumns, displayActionsCol, displayBulkSelectCol, attributeColumnsOder]);

export default useVisibleColumnOrder;
