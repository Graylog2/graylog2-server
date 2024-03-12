import * as React from 'react';
import type * as Immutable from 'immutable';

import { IfPermitted, } from 'components/common';
import IfInteractive from 'views/components/dashboard/IfInteractive';
import type { EventListItem } from 'views/components/widgets/events/types';
import usePluginEntities from 'hooks/usePluginEntities';
import RowActions from 'views/components/widgets/events/EventsList/RowActions';
import styled, { css } from 'styled-components';

const Td = styled.td(({ theme }) => css`
  && {
    border-color: ${theme.colors.table.row.border};
  }
`)

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
        const columnRenderer = (value: string ) => eventAttributes.find(({ attribute }) => attribute === field)?.displayValue?.(value) ?? value;

        return (
          <Td key={field}>
            {columnRenderer ? columnRenderer(value) : value}
          </Td>
        );
      })}
      <IfInteractive>
        <IfPermitted permissions="events:edit">
          <Td>
            <RowActions />
          </Td>
        </IfPermitted>
      </IfInteractive>
    </tr>
  );
};

export default EventsTableRow;
