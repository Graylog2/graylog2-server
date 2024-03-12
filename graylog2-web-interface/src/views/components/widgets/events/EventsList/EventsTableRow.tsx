import * as React from 'react';
import type * as Immutable from 'immutable';
import styled, { css } from 'styled-components';

import { IfPermitted, Timestamp } from 'components/common';
import IfInteractive from 'views/components/dashboard/IfInteractive';
import type { EventListItem } from 'views/components/widgets/events/types';
import usePluginEntities from 'hooks/usePluginEntities';

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
