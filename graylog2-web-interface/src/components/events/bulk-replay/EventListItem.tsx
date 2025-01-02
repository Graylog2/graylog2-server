import * as React from 'react';

import type { Event } from './types';

type EventListItemProps = {
  event: Event,
  done: boolean,
  selected: boolean,
  onClick: () => void,
  removeItem: (id: string) => void,
  markItemAsDone: (id: string) => void,
}
const EventListItem = ({ event, onClick, selected, removeItem, markItemAsDone }: EventListItemProps) => (
  <li key={`event-replay-list-${event?.id}`}>
    {selected ? '-' : ''}
    <a onClick={onClick}>{event?.message ?? <i>Unknown</i>}</a>

    <button onClick={() => removeItem(event.id)}>x</button>
    <button onClick={() => markItemAsDone(event.id)}>done</button>
  </li>
);

export default EventListItem;
