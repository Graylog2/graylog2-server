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
import type { SetStateAction } from 'react';
import { useMemo, useState, useCallback } from 'react';
import isFunction from 'lodash/isFunction';

import SelectEntitiesContext from './SelectEntitiesContext';

import type { EntityBase } from '../types';

type Props<Entity extends EntityBase> = React.PropsWithChildren<{
  initialSelection?: Array<string>,
  onChangeSelection: (selectedEntities: Array<Entity['id']>) => void
}>

const SelectedEntitiesProvider = <Entity extends EntityBase>({ children, initialSelection, onChangeSelection }: Props<Entity>) => {
  const [selectedEntities, setSelectedEntities] = useState<Array<Entity['id']>>(initialSelection);

  const _setSelectedEntities = useCallback((setSelectedEntitiesArgument: SetStateAction<Array<Entity['id']>>) => {
    const newState = isFunction(setSelectedEntitiesArgument) ? setSelectedEntitiesArgument(selectedEntities) : setSelectedEntitiesArgument;

    setSelectedEntities(newState);
    if (onChangeSelection) onChangeSelection(newState);
  }, [onChangeSelection, selectedEntities]);

  const deselectEntity = useCallback((targetEntityId: EntityBase['id']) => (
    _setSelectedEntities((cur) => cur.filter((entityId) => entityId !== targetEntityId))
  ), [_setSelectedEntities]);

  const selectEntity = useCallback((targetEntityId: EntityBase['id']) => (
    _setSelectedEntities((cur) => [...cur, targetEntityId])
  ), [_setSelectedEntities]);

  const contextValue = useMemo(() => ({
    setSelectedEntities: _setSelectedEntities,
    selectedEntities,
    deselectEntity,
    selectEntity,
  }), [
    _setSelectedEntities,
    selectedEntities,
    deselectEntity,
    selectEntity,
  ]);

  return (
    <SelectEntitiesContext.Provider value={contextValue}>
      {children}
    </SelectEntitiesContext.Provider>
  );
};

SelectedEntitiesProvider.defaultProps = {
  initialSelection: [],
};

export default SelectedEntitiesProvider;
