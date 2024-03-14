import * as React from 'react';
import type * as Immutable from 'immutable';
import styled, { css } from 'styled-components';

import IfInteractive from 'views/components/dashboard/IfInteractive';
import type { EventListItem } from 'views/components/widgets/events/types';
import usePluginEntities from 'hooks/usePluginEntities';
import RowActions from 'views/components/widgets/events/EventsList/RowActions';

const Td = styled.td(({ theme }) => css`
  && {
    border-color: ${theme.colors.table.row.border};
  }
`);

type Props = {
  event: EventListItem,
  fields: Immutable.OrderedSet<string>,
}

const EventsTableRow = ({ event, fields }: Props) => {
  const eventAttributes = usePluginEntities('views.components.widgets.events.attributes');

  return (
    <tr key={event.id}>
      {fields.toArray().map((field) => {
        const value = event[field];
        const columnRenderer = (val: string) => eventAttributes.find(
          ({ attribute }) => attribute === field,
        )?.displayValue?.(val) ?? val;

        return (
          <Td key={field}>
            {columnRenderer(value)}
          </Td>
        );
      })}
      <IfInteractive>
        <Td>
          <RowActions eventId={event.id} hasReplayInfo={!!event.replay_info} />
        </Td>
      </IfInteractive>
    </tr>
  );
};

export default EventsTableRow;
