import React from 'react';

import Direction from 'views/logic/aggregationbuilder/Direction';
import { SortIcon } from 'components/common';
import EventsWidgetSortConfig from 'views/logic/widgets/events/EventsWidgetSortConfig';

type Props = {
  activeSort: EventsWidgetSortConfig,
  field: string,
  fieldTitle: string,
  onSortChange: (newSortConfig: EventsWidgetSortConfig) => Promise<unknown>,
  setLoadingState: (loading: boolean) => void,
};

type DirectionStrategy = {
  handleSortChange: (changeSort: (direction: Direction, activeSort: boolean) => void) => void,
  tooltip: (fieldName: string) => string,
};

const _tooltip = (fieldName: string, newDirection: Direction) => `Sort ${fieldName} ${newDirection.direction}`;

const _changeSort = (
  nextDirection: Direction,
  fieldName: string, onSortChange: (newSortConfig: EventsWidgetSortConfig) => Promise<unknown>,
  setLoadingState: (loading: boolean) => void,
) => {
  setLoadingState(true);

  onSortChange(new EventsWidgetSortConfig(fieldName, nextDirection)).then(() => {
    setLoadingState(false);
  });
};

const DirectionStrategyAsc: DirectionStrategy = {
  tooltip: (fieldName: string) => _tooltip(fieldName, Direction.Descending),
  handleSortChange: (changeSort) => changeSort(Direction.Descending, true),
};

const DirectionStrategyDesc: DirectionStrategy = {
  tooltip: (fieldName: string) => _tooltip(fieldName, Direction.Ascending),
  handleSortChange: (changeSort) => changeSort(Direction.Ascending, true),
};

const DirectionStrategyNoSort: DirectionStrategy = {
  tooltip: (fieldName: string) => _tooltip(fieldName, Direction.Ascending),
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

const AttributeSortIcon = ({ field, fieldTitle, onSortChange, setLoadingState, activeSort }: Props) => {
  const activeDirection = activeSort.field === field ? activeSort.direction : undefined;
  const { tooltip, handleSortChange }: DirectionStrategy = directionStrategy(activeDirection);
  const changeSort = (nextDirection: Direction) => _changeSort(nextDirection, field, onSortChange, setLoadingState);
  const title = tooltip(fieldTitle);

  return (
    <SortIcon onChange={() => handleSortChange(changeSort)} activeDirection={activeDirection?.direction} title={title} />
  );
};

export default AttributeSortIcon;
