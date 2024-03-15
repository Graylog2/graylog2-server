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

import Direction from 'views/logic/aggregationbuilder/Direction';
import { SortIcon } from 'components/common';

type Props = {
  activeDirection: Direction,
  activeAttribute: string,
  attribute: string,
  attributeTitle: string,
  onSortChange: (attribute: string, nextDirection: Direction) => Promise<unknown>,
  setLoadingState: (loading: boolean) => void,
};

type DirectionStrategy = {
  handleSortChange: (changeSort: (direction: Direction, activeSort: boolean) => void) => void,
  tooltip: (attributeTitle: string) => string,
};

const _tooltip = (attributeTitle: string, newDirection: Direction) => `Sort ${attributeTitle} ${newDirection.direction}`;

const _changeSort = (
  nextDirection: Direction,
  attribute: string,
  onSortChange: (attribute: string, nextDirection: Direction) => Promise<unknown>,
  setLoadingState: (loading: boolean) => void,
) => {
  setLoadingState(true);

  onSortChange(attribute, nextDirection).then(() => {
    setLoadingState(false);
  });
};

const DirectionStrategyAsc: DirectionStrategy = {
  tooltip: (attributeTitle: string) => _tooltip(attributeTitle, Direction.Descending),
  handleSortChange: (changeSort) => changeSort(Direction.Descending, true),
};

const DirectionStrategyDesc: DirectionStrategy = {
  tooltip: (attributeTitle: string) => _tooltip(attributeTitle, Direction.Ascending),
  handleSortChange: (changeSort) => changeSort(Direction.Ascending, true),
};

const DirectionStrategyNoSort: DirectionStrategy = {
  tooltip: (attributeTitle: string) => _tooltip(attributeTitle, Direction.Ascending),
  handleSortChange: (changeSort) => changeSort(Direction.Ascending, true),
};

const directionStrategy = (activeDirection: Direction | undefined) => {
  switch (activeDirection?.direction) {
    case Direction.Ascending.direction:
      return DirectionStrategyAsc;
    case Direction.Descending.direction:
      return DirectionStrategyDesc;
    default:
      return DirectionStrategyNoSort;
  }
};

const AttributeSortIcon = ({ attribute, attributeTitle, onSortChange, setLoadingState, activeDirection, activeAttribute }: Props) => {
  const targetAttributeDirection = activeAttribute === attribute ? activeDirection : undefined;
  const { tooltip, handleSortChange }: DirectionStrategy = directionStrategy(activeDirection);
  const changeSort = (nextDirection: Direction) => _changeSort(nextDirection, attribute, onSortChange, setLoadingState);
  const title = tooltip(attributeTitle);

  return (
    <SortIcon onChange={() => handleSortChange(changeSort)} activeDirection={targetAttributeDirection?.direction} title={title} />
  );
};

export default AttributeSortIcon;
