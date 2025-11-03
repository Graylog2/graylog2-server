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

import { UTILITY_COLUMNS, BULK_SELECT_COL_ID, ACTIONS_COL_ID } from 'components/common/EntityDataTable/Constants';

const useColumnOrder = (visibleColumns: Array<string>, attributeColumnsOder: Array<string>) =>
  useMemo(() => {
    const visibleSet = new Set(visibleColumns);
    const coreOrder = attributeColumnsOder.filter((id) => visibleSet.has(id));
    // Display columns, which are not part of the defined order, at the end of the table (before actions column)
    const additionalVisible = visibleColumns.filter(
      (id) => !UTILITY_COLUMNS.has(id) && !attributeColumnsOder.includes(id),
    );

    return [
      visibleSet.has(BULK_SELECT_COL_ID) ? BULK_SELECT_COL_ID : null,
      ...coreOrder,
      ...additionalVisible,
      visibleSet.has(ACTIONS_COL_ID) ? ACTIONS_COL_ID : null,
    ].filter(Boolean);
  }, [visibleColumns, attributeColumnsOder]);

export default useColumnOrder;
