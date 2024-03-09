import * as React from 'react';
import type * as Immutable from 'immutable';
import styled, { css } from 'styled-components';

import { IfPermitted, Timestamp } from 'components/common';
import IfInteractive from 'views/components/dashboard/IfInteractive';
import type { EventListItem } from 'views/components/widgets/events/types';

const columnRenderersByType = {
  date: (date: string) => <Timestamp dateTime={date} />,
};

const Name = styled.button(({ theme }) => css`
  background: none;
  border: none;
  padding: 0;
  color: ${theme.colors.global.link};

  &:hover {
    color: ${theme.colors.global.linkHover};
    text-decoration: underline;
  }
`);

const useColumnRenderers = () => ({
  created_at: columnRenderersByType.date,
  updated_at: columnRenderersByType.date,
  name: (name: string) => (
    <Name onClick={() => {}} type="button">
      {name}
    </Name>
  ),
});

type Props = {
  event: EventListItem,
  fields: Immutable.OrderedSet<string>,
}

const EventsTableRow = ({ event, fields }: Props) => {
  const columnRenderers = useColumnRenderers();

  return (
    <tr key={event.id}>
      {fields.toArray().map((field) => {
        const value = event[field];
        const columnRenderer = columnRenderers[field];

        return (
          <td key={field}>
            {columnRenderer ? columnRenderer(value) : value}
          </td>
        );
      })}
      <IfInteractive>
        <IfPermitted permissions="events:edit">
          <td>
            Actions
          </td>
        </IfPermitted>
      </IfInteractive>
    </tr>
  );
};

export default EventsTableRow;
