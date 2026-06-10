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
import { useMemo, useCallback } from 'react';
import type { Table } from '@tanstack/react-table';

import SelectEntitiesContext from './SelectEntitiesContext';

import type { EntityBase } from '../types';

type Props<Entity extends EntityBase> = React.PropsWithChildren<{
  isAllRowsSelected: boolean;
  isSomeRowsSelected: boolean;
  selectedEntities: Array<Entity['id']>;
  table: Table<Entity>;
}>;

const SelectedEntitiesProvider = <Entity extends EntityBase>({
  children = undefined,
  isAllRowsSelected,
  isSomeRowsSelected,
  selectedEntities,
  table,
}: Props<Entity>) => {
  const deselectEntity = useCallback(
    (targetEntityId: EntityBase['id']) => table.setRowSelection((cur) => ({ ...cur, [targetEntityId]: false })),
    [table],
  );

  const selectEntity = useCallback(
    (targetEntityId: EntityBase['id']) => table.setRowSelection((cur) => ({ ...cur, [targetEntityId]: true })),
    [table],
  );

  const toggleEntitySelect = useCallback(
    (targetEntityId: EntityBase['id']) =>
      table.setRowSelection((cur) => ({ ...cur, [targetEntityId]: !cur[targetEntityId] })),
    [table],
  );

  const contextValue = useMemo(
    () => ({
      setSelectedEntities: (rows: Array<string>) =>
        table.setRowSelection(Object.fromEntries(rows.map((id) => [id, true]))),
      selectedEntities,
      deselectEntity,
      selectEntity,
      toggleEntitySelect,
      isAllRowsSelected,
      isSomeRowsSelected,
    }),
    [selectedEntities, deselectEntity, selectEntity, toggleEntitySelect, table, isAllRowsSelected, isSomeRowsSelected],
  );

  return <SelectEntitiesContext.Provider value={contextValue}>{children}</SelectEntitiesContext.Provider>;
};

export default SelectedEntitiesProvider;
