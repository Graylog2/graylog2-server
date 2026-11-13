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
import { useState } from 'react';

import SelectEntitiesContext from 'components/common/EntityDataTable/contexts/SelectEntitiesContext';

type Props = React.PropsWithChildren<{
  rowIds: Array<string>;
}>;

const DataTableSelectedEntitiesProvider = ({ rowIds, children = undefined }: Props) => {
  const [selection, setSelection] = useState<Array<string>>([]);
  const selectionSet = new Set(selection);
  // Ignores selected rows which are no longer part of the result.
  const selectedEntities = rowIds.filter((id) => selectionSet.has(id));
  const isAllRowsSelected = rowIds.length > 0 && selectedEntities.length === rowIds.length;

  const deselectEntity = (id: string) => setSelection((cur) => cur.filter((curId) => curId !== id));
  const selectEntity = (id: string) => setSelection((cur) => [...cur, id]);
  const toggleEntitySelect = (id: string) =>
    setSelection((cur) => (cur.includes(id) ? cur.filter((curId) => curId !== id) : [...cur, id]));

  const contextValue = {
    selectedEntities,
    setSelectedEntities: setSelection,
    selectEntity,
    deselectEntity,
    toggleEntitySelect,
    isAllRowsSelected,
    isSomeRowsSelected: selectedEntities.length > 0 && !isAllRowsSelected,
  };

  return <SelectEntitiesContext.Provider value={contextValue}>{children}</SelectEntitiesContext.Provider>;
};

export default DataTableSelectedEntitiesProvider;
