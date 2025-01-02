import * as React from 'react';
import styled, { css } from 'styled-components';

import IconButton from 'components/common/IconButton';
import ButtonGroup from 'components/bootstrap/ButtonGroup';

import type { Event } from './types';

type EventListItemProps = {
  event: Event,
  done: boolean,
  selected: boolean,
  onClick: () => void,
  removeItem: (id: string) => void,
  markItemAsDone: (id: string) => void,
}

type StyledItemProps = {
  $selected: boolean;
}
const StyledItem = styled.li<StyledItemProps>(({ theme, $selected }) => css`
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 30px;
  background-color: ${$selected ? theme.colors.background.secondaryNav : 'transparent'};
`);

type SummaryProps = {
  $done: boolean;
}

const Summary = styled.span<SummaryProps>(({ theme, $done }) => css`
  margin: 10px;
  cursor: pointer;
  color: ${$done ? theme.colors.global.textSecondary : theme.colors.global.textDefault};
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  &:hover {
    text-decoration: underline;
  }
`);

const EventListItem = ({ done, event, onClick, selected, removeItem, markItemAsDone }: EventListItemProps) => (
  <StyledItem key={`event-replay-list-${event?.id}`} $selected={selected}>
    <Summary $done={done} onClick={onClick}>{event?.message ?? <i>Unknown</i>}</Summary>

    <ButtonGroup>
      <IconButton onClick={() => removeItem(event.id)} title="Remove event from list" name="delete" />
      <IconButton onClick={() => markItemAsDone(event.id)} title="Mark event as investigated" name="check" />
    </ButtonGroup>
  </StyledItem>
);

export default EventListItem;
