import * as React from 'react';
import { useCallback } from 'react';
import styled, { css } from 'styled-components';

import IconButton from 'components/common/IconButton';
import ButtonGroup from 'components/bootstrap/ButtonGroup';
import type { Event } from 'components/events/events/types';

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
  cursor: pointer;

  &:hover {
    background-color: ${theme.colors.background.body};
  }
`);

type SummaryProps = {
  $done: boolean;
}

const Summary = styled.span<SummaryProps>(({ theme, $done }) => css`
  margin: 10px;
  color: ${$done ? theme.colors.global.textSecondary : theme.colors.global.textDefault};
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  &:hover {
    text-decoration: underline;
  }
`);

const EventListItem = ({ done, event, onClick, selected, removeItem, markItemAsDone }: EventListItemProps) => {
  const _removeItem = useCallback((e: React.MouseEvent<HTMLButtonElement>) => {
    e.stopPropagation();

    return removeItem(event.id);
  }, [event?.id, removeItem]);
  const _markItemAsDone = useCallback((e: React.MouseEvent<HTMLButtonElement>) => {
    e.stopPropagation();

    return markItemAsDone(event.id);
  }, [event?.id, markItemAsDone]);

  return (
    <StyledItem key={`event-replay-list-${event?.id}`} $selected={selected} onClick={onClick}>
      <Summary $done={done}>{event?.message ?? <i>Unknown</i>}</Summary>

      <ButtonGroup>
        <IconButton onClick={_removeItem} title="Remove event from list" name="delete" />
        <IconButton onClick={_markItemAsDone} title="Mark event as investigated" name="check" />
      </ButtonGroup>
    </StyledItem>
  );
};

export default EventListItem;
