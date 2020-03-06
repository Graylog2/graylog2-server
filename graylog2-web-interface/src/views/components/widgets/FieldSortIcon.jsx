// @flow strict
import React from 'react';
import styled, { css, type StyledComponent } from 'styled-components';
import PropTypes from 'prop-types';

import MessagesWidgetConfig, { defaultSortDirection } from 'views/logic/widgets/MessagesWidgetConfig';
import Direction, { type DirectionJson } from 'views/logic/aggregationbuilder/Direction';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import CustomPropTypes from 'views/components/CustomPropTypes';

import { Icon } from 'components/common';

type Props = {
  config: MessagesWidgetConfig,
  fieldName: string,
  onConfigChange: (MessagesWidgetConfig) => Promise<void>,
  setLoadingState: (loading: boolean) => void,
}

const SortIcon: StyledComponent<{fieldSortActive: boolean}, {}, HTMLButtonElement> = styled.button(({ fieldSortActive }) => {
  const color = fieldSortActive ? '#333' : '#bdbdbd';

  return css`
    border: 0;
    background: transparent;
    color: ${color};

    padding: 5px;

    cursor: pointer;
  `;
});

const _newSort = (config, fieldName, currentSortDirectioName) => {
  switch (currentSortDirectioName) {
    case Direction.Ascending.direction:
      return config.sort.map(sort => sort.toBuilder().direction(Direction.Descending).build());
    case Direction.Descending.direction:
      return config.sort.map(sort => sort.toBuilder().direction(Direction.Ascending).build());
    default:
      return [new SortConfig(SortConfig.PIVOT_TYPE, fieldName, defaultSortDirection)];
  }
};

const _changeSort = (config: MessagesWidgetConfig, fieldName: string, sortDirectionName: ?DirectionJson, onConfigChange: (MessagesWidgetConfig) => Promise<void>, setLoadingState: (loading: boolean) => void) => {
  setLoadingState(true);
  const newSort = _newSort(config, fieldName, sortDirectionName);
  const newConfig = config.toBuilder().sort(newSort).build();
  onConfigChange(newConfig).then(() => {
    setLoadingState(false);
  });
};

const _sortDirectionName = (config: MessagesWidgetConfig, fieldName: string) => {
  const currentSort = config.sort && config.sort.length > 0 ? config.sort[0] : null;
  if (currentSort && currentSort.field === fieldName) {
    return currentSort.direction.direction;
  }
  return null;
};

const _newSortDescription = (fielName: string, sortDirectionName: ?DirectionJson) => {
  let newDirection;
  if (sortDirectionName && sortDirectionName === Direction.Ascending) {
    newDirection = Direction.Descending.direction;
  } else {
    newDirection = Direction.Ascending.direction;
  }
  return `Sort ${fielName} ${newDirection}`;
};

const _sortIcon = (fieldSortDirection: ?DirectionJson) => {
  if (fieldSortDirection && fieldSortDirection === Direction.Ascending.direction) {
    return 'sort-amount-asc';
  }
  return 'sort-amount-desc';
};


const FieldSortIcon = ({ fieldName, config, onConfigChange, setLoadingState }: Props) => {
  const sortDirectionName = _sortDirectionName(config, fieldName);
  const newSortDescription = _newSortDescription(fieldName, sortDirectionName);
  return (
    <SortIcon fieldSortActive={!!sortDirectionName}
              title={newSortDescription}
              aria-label={newSortDescription}
              onClick={() => _changeSort(config, fieldName, sortDirectionName, onConfigChange, setLoadingState)}
              data-testid="messages-sort-icon">
      <Icon name={_sortIcon(sortDirectionName)} />
    </SortIcon>
  );
};

FieldSortIcon.propTypes = {
  config: CustomPropTypes.instanceOf(MessagesWidgetConfig).isRequired,
  fieldName: PropTypes.string.isRequired,
  onConfigChange: PropTypes.func.isRequired,
  setLoadingState: PropTypes.func.isRequired,
};

export default FieldSortIcon;
