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
import React from 'react';

import type MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import { defaultSortDirection } from 'views/logic/widgets/MessagesWidgetConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import { SortIcon } from 'components/common';

type Props = {
  config: MessagesWidgetConfig,
  fieldName: string,
  onSortChange: (newSortConfig: SortConfig[]) => Promise<void>,
  setLoadingState: (loading: boolean) => void,
};

type DirectionStrategy = {
  handleSortChange: (changeSort: (direction: Direction) => void) => void,
  tooltip: (fieldName: string) => string,
};

const _tooltip = (fieldName: string, newDirection: Direction) => `Sort ${fieldName} ${newDirection.direction}`;

const _changeSort = (nextDirection: Direction, _config: MessagesWidgetConfig, fieldName: string, onSortChange: (newSortConfig: SortConfig[]) => Promise<void>, setLoadingState: (loading: boolean) => void) => {
  const newSort = [new SortConfig(SortConfig.PIVOT_TYPE, fieldName, nextDirection)];

  setLoadingState(true);

  onSortChange(newSort).then(() => {
    setLoadingState(false);
  });
};

const _isFieldSortActive = (config: MessagesWidgetConfig, fieldName: string) => config.sort && config.sort.length > 0 && config.sort[0].field === fieldName;

const DirectionStrategyAsc: DirectionStrategy = {
  tooltip: (fieldName: string) => _tooltip(fieldName, Direction.Descending),
  handleSortChange: (changeSort) => changeSort(Direction.Descending),
};

const DirectionStrategyDesc: DirectionStrategy = {
  tooltip: (fieldName: string) => _tooltip(fieldName, Direction.Ascending),
  handleSortChange: (changeSort) => changeSort(Direction.Ascending),
};

const DirectionStrategyNoSort: DirectionStrategy = {
  tooltip: (fieldName: string) => _tooltip(fieldName, defaultSortDirection),
  handleSortChange: (changeSort) => changeSort(defaultSortDirection),
};

const _directionStrategy = (config: MessagesWidgetConfig, fieldName: string) => {
  const fieldSortDirection = _isFieldSortActive(config, fieldName) ? config.sort[0].direction.direction : null;

  switch (fieldSortDirection) {
    case Direction.Ascending.direction:
      return DirectionStrategyAsc;
    case Direction.Descending.direction:
      return DirectionStrategyDesc;
    default:
      return DirectionStrategyNoSort;
  }
};

const FieldSortIcon = ({ fieldName, config, onSortChange, setLoadingState }: Props) => {
  const changeSort = (nextDirection: Direction) => _changeSort(nextDirection, config, fieldName, onSortChange, setLoadingState);

  const activeDirection = _isFieldSortActive(config, fieldName) ? config.sort[0].direction.direction : null;
  const { tooltip, handleSortChange }: DirectionStrategy = _directionStrategy(config, fieldName);

  return (
    <SortIcon activeDirection={activeDirection}
              onChange={() => handleSortChange(changeSort)}
              title={tooltip(fieldName)} />
  );
};

export default FieldSortIcon;
