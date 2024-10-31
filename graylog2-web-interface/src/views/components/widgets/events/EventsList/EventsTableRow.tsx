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
import * as React from 'react';
import type * as Immutable from 'immutable';
import styled, { css } from 'styled-components';

import IfInteractive from 'views/components/dashboard/IfInteractive';
import type { EventListItem } from 'views/components/widgets/events/types';
import RowActions from 'views/components/widgets/events/EventsList/RowActions';
import useEventAttributes from 'views/components/widgets/events/hooks/useEventAttributes';

const Td = styled.td(({ theme }) => css`
  && {
    border-color: ${theme.colors.table.row.divider};
  }
`);

type Props = {
  event: EventListItem,
  fields: Immutable.OrderedSet<string>,
}

const EventsTableRow = ({ event, fields }: Props) => {
  const eventAttributes = useEventAttributes();

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
          <RowActions eventId={event.id} eventDefinitionId={event.event_definition_id} hasReplayInfo={!!event.replay_info} />
        </Td>
      </IfInteractive>
    </tr>
  );
};

export default EventsTableRow;
