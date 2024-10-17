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
import React, { useMemo } from 'react';
import type { OrderedMap } from 'immutable';

import Direction from 'views/logic/aggregationbuilder/Direction';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import { SortIcon } from 'components/common';

type Props = {
  sortConfigMap: OrderedMap<string, SortConfig>,
  fieldName: string,
  onSortChange: (newSortConfig: SortConfig[]) => Promise<unknown>,
  setLoadingState: (loading: boolean) => void,
  type: 'pivot' | 'series' | undefined
};

type DirectionStrategy = {
  handleSortChange: (changeSort: (direction: Direction, activeSort: boolean) => void) => void,
  tooltip: (fieldName: string) => string,
};

const _tooltip = (fieldName: string, newDirection: Direction | null) => (newDirection ? `Sort ${fieldName} ${newDirection.direction}` : `Remove ${fieldName} sort`);

const _changeSort = (nextDirection: Direction, sortConfigMap: OrderedMap<string, SortConfig>, fieldName: string, onSortChange: (newSortConfig: SortConfig[]) => Promise<unknown>, setLoadingState: (loading: boolean) => void, type, activeSort) => {
  let newSortConfigSet;

  if (activeSort) {
    newSortConfigSet = sortConfigMap.set(fieldName, new SortConfig(type, fieldName, nextDirection));
  } else {
    newSortConfigSet = sortConfigMap.delete(fieldName);
  }

  setLoadingState(true);

  onSortChange(newSortConfigSet.toList().toArray()).then(() => {
    setLoadingState(false);
  });
};

const _isFieldSortActive = (sortConfigMap: OrderedMap<string, SortConfig>, fieldName: string) => sortConfigMap.get(fieldName);

const DirectionStrategyAsc: DirectionStrategy = {
  tooltip: (fieldName: string) => _tooltip(fieldName, Direction.Descending),
  handleSortChange: (changeSort) => changeSort(Direction.Descending, true),
};

const DirectionStrategyDesc: DirectionStrategy = {
  tooltip: (fieldName: string) => _tooltip(fieldName, null),
  handleSortChange: (changeSort) => changeSort(Direction.Descending, false),
};

const DirectionStrategyNoSort: DirectionStrategy = {
  tooltip: (fieldName: string) => _tooltip(fieldName, Direction.Ascending),
  handleSortChange: (changeSort) => changeSort(Direction.Ascending, true),
};

const _directionStrategy = (activeDirection) => {
  switch (activeDirection) {
    case Direction.Ascending.direction:
      return DirectionStrategyAsc;
    case Direction.Descending.direction:
      return DirectionStrategyDesc;
    default:
      return DirectionStrategyNoSort;
  }
};

const FieldSortIcon = ({ fieldName, type, sortConfigMap, onSortChange, setLoadingState }: Props) => {
  const changeSort = (nextDirection: Direction, activeSort: boolean) => _changeSort(nextDirection, sortConfigMap, fieldName, onSortChange, setLoadingState, type, activeSort);
  const activeDirection = _isFieldSortActive(sortConfigMap, fieldName)?.direction?.direction;
  const { tooltip, handleSortChange }: DirectionStrategy = _directionStrategy(activeDirection);
  const order = useMemo(() => {
    if (sortConfigMap.size < 2) return undefined;
    const index = sortConfigMap.keySeq().findIndex((k) => k === fieldName) + 1;

    return index || undefined;
  }, [fieldName, sortConfigMap]);
  const title = tooltip(fieldName);

  return (
    <SortIcon onChange={() => handleSortChange(changeSort)} activeDirection={activeDirection} title={title} order={order} />
  );
};

export default FieldSortIcon;
